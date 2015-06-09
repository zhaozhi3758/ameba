package ameba.mvc.template.internal;

import groovy.lang.Singleton;
import jersey.repackaged.com.google.common.collect.Sets;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;
import org.glassfish.jersey.spi.ExtendedExceptionMapper;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Set;

/**
 * 404 跳转到模板
 *
 * @author 张立鑫 IntelligentCode
 * @since 2013-08-27
 */
@Singleton
public class NotFoundForward implements ExtendedExceptionMapper<NotFoundException> {

    @Context
    private Provider<UriInfo> uriInfo;
    @Inject
    private ServiceLocator serviceLocator;
    private ThreadLocal<String> templatePath = new ThreadLocal<>();

    private Set<TemplateProcessor> getTemplateProcessors() {
        Set<TemplateProcessor> templateProcessors = Sets.newLinkedHashSet();

        templateProcessors.addAll(Providers.getCustomProviders(serviceLocator, TemplateProcessor.class));
        templateProcessors.addAll(Providers.getProviders(serviceLocator, TemplateProcessor.class));
        return templateProcessors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response toResponse(NotFoundException exception) {
        try {
            return Response.ok(Viewables.newDefaultViewable(templatePath.get())).type(MediaType.TEXT_HTML_TYPE).build();
        } finally {
            templatePath.remove();
        }
    }

    private String getCurrentPath() {
        return "/" + uriInfo.get().getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMappable(NotFoundException exception) {
        String path = getCurrentPath();
        //受保护目录,不允许直接访问
        String pDir = Viewables.PROTECTED_DIR + "/";
        if (path.startsWith(pDir)
                || path.startsWith("/" + pDir)) return false;
        for (TemplateProcessor templateProcessor : getTemplateProcessors()) {
            Object has = templateProcessor.resolve(path, null);
            if (has == null) {
                path = path + (path.endsWith("/") ? "" : "/") + "index";
                has = templateProcessor.resolve(path, null);
            }
            if (has != null) {
                templatePath.set(path);
                return true;
            }
        }
        return false;
    }
}
