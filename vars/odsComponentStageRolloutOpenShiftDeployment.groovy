import org.ods.component.RolloutOpenShiftDeploymentStage
import org.ods.component.IContext

import org.ods.services.OpenShiftService
import org.ods.services.JenkinsService
import org.ods.services.ServiceRegistry
import org.ods.util.Logger
import org.ods.util.ILogger

def call(IContext context, Map config = [:]) {
    ILogger logger = ServiceRegistry.instance.get(Logger)
    // this is only for testing, because we need access to the script context :(
    if (!logger) {
        logger = new Logger (this, !!env.DEBUG)
    }
    def openShiftService = ServiceRegistry.instance.get(OpenShiftService)
    if (config.targetProject) {
        openShiftService = new OpenShiftService(
            new PipelineSteps(this),
            logger,
            config.targetProject
        )
    }
    return new RolloutOpenShiftDeploymentStage(
        this,
        context,
        config,
        openShiftService,
        ServiceRegistry.instance.get(JenkinsService),
        logger
    ).execute()
}
return this
