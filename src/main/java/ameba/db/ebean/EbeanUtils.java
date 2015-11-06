package ameba.db.ebean;

import ameba.core.Requests;
import ameba.db.ebean.jackson.CommonBeanSerializer;
import ameba.message.filtering.EntityFieldsUtils;
import ameba.message.internal.PathProperties.Each;
import ameba.message.internal.PathProperties.Props;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.Query;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.common.BeanMap;
import com.avaje.ebean.text.PathProperties;
import com.avaje.ebean.text.json.JsonContext;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.properties.BeanPropertiesReader;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.BadRequestException;
import java.util.*;

import static com.avaje.ebean.OrderBy.Property;

/**
 * <p>EbeanUtils class.</p>
 *
 * @author icode
 * @since 0.1.6e
 */
public class EbeanUtils {
    public static final String PATH_PROPS_PARSED = EbeanUtils.class + ".PathProperties";
    private static final String PARSE_ORDER_ERR_MSG = "Parse OrderBy error. OrderBy";
    private static final String[] UNSAFE_SQL_STRING = {
            "'", "/", "*", "%", ";", "+", "(", ")", ",", "--", "%", "#"
    };

    private static final Map<Class, String[]> BEAN_PROPS_CACHE = new WeakHashMap<>();


    private EbeanUtils() {
    }

    /**
     * <p>forceUpdateAllProperties.</p>
     *
     * @param server a {@link com.avaje.ebeaninternal.api.SpiEbeanServer} object.
     * @param model  a T object.
     * @param <T>    a T object.
     */
    @SuppressWarnings("unchecked")
    public static <T> void forceUpdateAllProperties(SpiEbeanServer server, T model) {
        forceUpdateAllProperties(server.getBeanDescriptor((Class<T>) model.getClass()), model);
    }

    /**
     * <p>forceUpdateAllProperties.</p>
     *
     * @param beanDescriptor a {@link com.avaje.ebeaninternal.server.deploy.BeanDescriptor} object.
     * @param model          a T object.
     * @param <T>            a T object.
     */
    public static <T> void forceUpdateAllProperties(BeanDescriptor<T> beanDescriptor, T model) {
        EntityBeanIntercept intercept = ((EntityBean) model)._ebean_getIntercept();
        intercept.setLoaded();
        int idIndex = beanDescriptor.getIdProperty().getPropertyIndex();
        for (int i = 0; i < intercept.getPropertyLength(); i++) {
            if (i != idIndex) {
                intercept.markPropertyAsChanged(i);
                intercept.setLoadedProperty(i);
            }
        }
    }

    /**
     * <p>forcePropertiesLoaded.</p>
     *
     * @param model a T object.
     * @param <T>   a T object.
     */
    public static <T> void forcePropertiesLoaded(T model) {
        if (model == null) return;
        if (model instanceof EntityBean) {
            EntityBeanIntercept intercept = ((EntityBean) model)._ebean_getIntercept();
            intercept.setLoaded();
            for (int i = 0; i < intercept.getPropertyLength(); i++) {
                intercept.setLoadedProperty(i);
            }
        } else if (model instanceof Collection) {
            for (Object m : (Collection) model) {
                forcePropertiesLoaded(m);
            }
        } else if (model instanceof BeanMap) {
            forcePropertiesLoaded(((BeanMap) model).values());
        } else if (model.getClass().isArray()) {
            for (Object m : (Object[]) model) {
                forcePropertiesLoaded(m);
            }
        }
    }

    /**
     * parse uri query param to PathProperties for Ebean.json().toJson()
     *
     * @return PathProperties
     * @see JsonContext#toJson(Object, JsonGenerator, PathProperties)
     * @see CommonBeanSerializer#serialize(Object, JsonGenerator, SerializerProvider)
     */
    public static PathProperties getCurrentRequestPathProperties() {
        PathProperties properties = (PathProperties) Requests.getProperty(PATH_PROPS_PARSED);
        if (properties == null) {
            final ameba.message.internal.PathProperties pathProperties = EntityFieldsUtils.parsePathProperties();
            final PathProperties finalProperties = properties = new PathProperties();
            pathProperties.each(new Each<String, Props>() {
                @Override
                public void execute(Props props) {
                    finalProperties.put(null, props.getProperties());
                }

                @Override
                public void execute(String s, Props props) {
                    finalProperties.put(s, props.getProperties());
                }
            });
            Requests.setProperty(PATH_PROPS_PARSED, properties);
        }
        return properties;
    }

    public static <T> void appendOrder(OrderBy<T> orderBy, String orderByClause) {

        if (orderByClause == null) {
            return;
        }

        Query query = orderBy.getQuery();

        String[] properties = null;
        if (query == null) {
            checkSqlSafe(orderByClause, PARSE_ORDER_ERR_MSG);
        } else {
            Class beanType = query.getBeanType();
            if (beanType != null) {
                properties = getBeanProperties(beanType);
            }
        }
        String[] chunks = orderByClause.split(",");
        for (String chunk : chunks) {
            String[] pairs = chunk.split(" ");
            Property p = parseOrderProperty(pairs);
            if (p != null) {
                if (properties != null) {
                    String f = p.getProperty();
                    if (!ArrayUtils.contains(properties, f)) {
                        throw new BadRequestException(PARSE_ORDER_ERR_MSG + " can not found [" + f + "] field.");
                    }
                }
                orderBy.add(p);
            }
        }
    }

    public static String[] getBeanProperties(Class beanType) {
        String[] props = BEAN_PROPS_CACHE.get(beanType);
        if (props == null) {
            BeanPropertiesReader reader = new BeanPropertiesReader(beanType);
            props = reader.getProperties();
            BEAN_PROPS_CACHE.put(beanType, props);
        }
        return props;
    }

    public static void checkSqlSafe(String clause, String errorMsg) {
        for (String tag : UNSAFE_SQL_STRING) {
            if (clause.contains(tag)) {
                throw new BadRequestException(errorMsg + " can not contains `" + tag + "` in [" + clause + "].");
            }
        }
    }

    private static Property parseOrderProperty(String[] pairs) {
        if (pairs.length == 0) {
            return null;
        }

        ArrayList<String> wordList = Lists.newArrayListWithCapacity(pairs.length);
        for (String pair : pairs) {
            if (StringUtils.isNotBlank(pair)) {
                wordList.add(pair);
            }
        }
        if (wordList.isEmpty()) {
            return null;
        }
        String field = wordList.get(0);
        if (wordList.size() == 1) {
            if (field.startsWith("-")) {
                return new Property(field.substring(1), false);
            } else {
                return new Property(field, true);
            }
        }
        if (wordList.size() == 2) {
            boolean asc = isOrderAscending(wordList.get(1));
            return new Property(field, asc);
        }
        String m = "Parse OrderBy error. Expecting a max of 2 words in [" + Arrays.toString(pairs)
                + "] but got " + wordList.size();
        throw new BadRequestException(m);
    }

    private static boolean isOrderAscending(String s) {
        s = s.toLowerCase();
        if (s.startsWith("asc")) {
            return true;
        }
        if (s.startsWith("desc")) {
            return false;
        }
        throw new BadRequestException("Parse OrderBy error. Expecting [" + s + "] to be asc or desc?");
    }
}
