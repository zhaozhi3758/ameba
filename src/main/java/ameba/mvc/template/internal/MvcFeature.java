package ameba.mvc.template.internal;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.server.mvc.internal.ErrorTemplateExceptionMapper;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import java.util.Map;
import java.util.regex.Pattern;

import static org.glassfish.jersey.server.mvc.MvcFeature.CACHE_TEMPLATES;
import static org.glassfish.jersey.server.mvc.MvcFeature.TEMPLATE_BASE_PATH;

/**
 * <p>MvcFeature class.</p>
 *
 * @author 张立鑫 IntelligentCode
 * @since 2013-08-07
 */
public class MvcFeature implements Feature {

    private static final String TPL_CACHE = "template.caching";
    private static final String TPL_MODULE_DIR_PR = "template.directory.module.";


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();
        if (!config.isRegistered(ErrorTemplateExceptionMapper.class)) {
            context.register(ErrorTemplateExceptionMapper.class);
            context.register(new MvcBinder());
        }

        Map<String, String> tempConf = Maps.newHashMap();

        for (String key : config.getPropertyNames()) {
            if (key.startsWith(TemplateHelper.TPL_ENGINE_DIR_PR)) {//模板引擎默认路径
                String engine = key.replaceFirst(Pattern.quote(
                        TemplateHelper.TPL_ENGINE_DIR_PR.substring(0, TemplateHelper.TPL_ENGINE_DIR_PR.length() - 1)
                ), "");
                String confKey = TEMPLATE_BASE_PATH + engine;
                String value = (String) config.getProperty(confKey);
                String append = (String) config.getProperty(key);
                value = TemplateHelper.getTemplateEngineDirConfig(value, engine.substring(1), context, tempConf);
                if (StringUtils.isBlank(value)) {
                    value = append;
                } else {
                    value += "," + append;
                }
                tempConf.put(confKey, value);
            } else if (key.startsWith(TPL_MODULE_DIR_PR)) {//模块自定义模板路径
                String confKey = key.replaceFirst(Pattern.quote(TPL_MODULE_DIR_PR), "");
                int i = confKey.indexOf(".");
                if (i != -1) {
                    String engine = confKey.substring(0, i);
                    confKey = TEMPLATE_BASE_PATH + "." + engine;
                    String value = (String) config.getProperty(confKey);
                    String append = (String) config.getProperty(key);
                    value = TemplateHelper.getTemplateEngineDirConfig(value, engine, context, tempConf);
                    if (StringUtils.isBlank(value)) {
                        value = append;
                    } else {
                        value += "," + append;
                    }
                    tempConf.put(confKey, value);
                }
            } else if (key.startsWith(TPL_CACHE + ".")) {
                tempConf.put(CACHE_TEMPLATES + key.replaceFirst(Pattern.quote(TPL_CACHE), ""),
                        (String) config.getProperty(key));
            }
        }

        for (String key : tempConf.keySet()) {
            context.property(key, tempConf.get(key));
        }

        context.property(TEMPLATE_BASE_PATH,
                config.getProperty(TemplateHelper.TPL_DIR));

        context.property(CACHE_TEMPLATES,
                config.getProperty(TPL_CACHE));

        return true;
    }

}
