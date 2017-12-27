package BlogHelm.patches.buildTypes

import jetbrains.buildServer.configs.kotlin.v2017_2.*
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2017_2.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, change the buildType with uuid = '2c559e38-b9b4-4dcf-a79f-2faa91c9f5af' (id = 'BlogHelm_DeployStage')
accordingly and delete the patch script.
*/
changeBuildType("2c559e38-b9b4-4dcf-a79f-2faa91c9f5af") {
    check(templates == arrayListOf<String>()) {
        "Unexpected templates: '$templates'"
    }
    templates = arrayListOf("BlogHelm_DeployTemplate")

    check(enablePersonalBuilds == false) {
        "Unexpected option value: enablePersonalBuilds = $enablePersonalBuilds"
    }
    enablePersonalBuilds = true

    check(type == BuildTypeSettings.Type.DEPLOYMENT) {
        "Unexpected option value: type = $type"
    }
    type = BuildTypeSettings.Type.REGULAR

    check(maxRunningBuilds == 1) {
        "Unexpected option value: maxRunningBuilds = $maxRunningBuilds"
    }
    maxRunningBuilds = 0

    params {
        remove {
            text("env", "", label = "Environment", description = "Select the environment to deploy to",
                  regex = "^test|acc|prod${'$'}", validationMessage = "Must be one of test acc prod")
        }
        remove {
            param("helm.host", "192.168.99.101:30200")
        }
    }

    vcs {

        check(showDependenciesChanges == true) {
            "Unexpected option value: showDependenciesChanges = $showDependenciesChanges"
        }
        showDependenciesChanges = false

        remove("BlogHelm_BlogHelm")
    }

    expectSteps {
        script {
            scriptContent = """
                IMAGE_TAG=${'$'}(cat artifacts/image-tag.txt)
                echo "Using version ${'$'}IMAGE_TAG"
                
                helm upgrade --install blog-helm-%env% \
                  ./artifacts/blog-helm-${'$'}{IMAGE_TAG}.tgz \
                  --set image.tag=${'$'}IMAGE_TAG \
                  --values ./artifacts/values-%env%.yaml \
                  --debug \
                  --wait
            """.trimIndent()
            dockerImage = "lachlanevenson/k8s-helm:v2.6.2"
            dockerRunParameters = "--rm -e HELM_HOST=%helm.host%"
        }
    }
    steps {
        items.removeAt(0)
        check(stepsOrder == arrayListOf<String>()) {
            "Unexpected build steps order: $stepsOrder"
        }
        stepsOrder = arrayListOf("RUNNER_1")
    }

    dependencies {
        remove("BlogHelm_CommitStage") {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                id = "ARTIFACT_DEPENDENCY_1"
                cleanDestination = true
                artifactRules = """
                    *.tgz => artifacts
                    *.txt => artifacts
                    values-*.yaml => artifacts
                """.trimIndent()
            }
        }

    }
}