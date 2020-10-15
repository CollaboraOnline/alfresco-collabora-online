package dk.magenta.libreOffice.online;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.extensibility.ExtensionModuleEvaluator;
import java.util.Map;

/**
 * @author DarkStar1.
 */
public class PageModuleEvaluator implements ExtensionModuleEvaluator{
    private static final Logger logger = LoggerFactory.getLogger(PageModuleEvaluator.class);
    public static final String  PAGE_ID = "pageId";

    /**
     * <p>Determines whether or not to apply a module. The module being processed will already have been matched to the
     * path being processed but an evaluator can still be used to only apply the module in certain circumstances. These
     * will typically be dictated by whether or not some data in the supplied model matches the criteria defined in the
     * supplied <code>evaluationProperties</code>.</p>
     *
     * @param context              The current {@link RequestContext}
     * @param evaluationProperties The evaluation properties defined in the module.
     * @return <code>true</code> if the module should be applied and <code>false</code> otherwise.
     */
    @Override
    public boolean applyModule(RequestContext context, Map<String, String> evaluationProperties) {
        String currPage = context.getPageId();
        String targetPage = evaluationProperties.get( PAGE_ID);

        logger.debug("__Current Page Id: " + currPage);
        logger.debug("__Target Page Id: " + targetPage);

        return (targetPage != null && targetPage.equals(currPage));
    }

    /**
     * <p>Returns the names of the required evaluation properties that are needed to successfully perform an evaluation.
     * This information is used when provided a user-interface that allows a {@link ExtensionModuleEvaluator} to be
     * dynamically configured for a module.</p>
     *
     * @return A String array containing the names of the properties that are required by the evaluator.
     */
    @Override
    public String[] getRequiredProperties() {
        return new String[] { PAGE_ID};
    }
}
