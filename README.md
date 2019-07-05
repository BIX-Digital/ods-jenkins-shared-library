# Jenkins Shared Library

This library allows to have a minimal Jenkinsfile in each repository by providing all language-agnostic build aspects. The goal is to duplicate as little as possible between repositories and have an easy way to ship updates to all projects.


## Usage

Load the shared library in your `Jenkinsfile` like this:
```groovy
def final projectId = "hugo"
def final componentId = "be-node-express"
def final credentialsId = "${projectId}-cd-cd-user-with-password"
def sharedLibraryRepository
def dockerRegistry
node {
  sharedLibraryRepository = env.SHARED_LIBRARY_REPOSITORY
  dockerRegistry = env.DOCKER_REGISTRY
}

library identifier: 'ods-library@production', retriever: modernSCM(
  [$class: 'GitSCMSource',
   remote: sharedLibraryRepository,
   credentialsId: credentialsId])

odsPipeline(
  image: "${dockerRegistry}/cd/jenkins-slave-maven",
  projectId: projectId,
  componentId: componentId,
  branchToEnvironmentMapping: [
    'master': 'test',
    '*': 'dev'
  ]
) { context ->
  stage('Build') {
      // custom stage
  }
  stageScanForSonarqube(context) // using a provided stage
}
```

## Provided Stages
Following stages are provided (see folder vars for more details):
* stageScanForSonarqube(context)
* stageOWASPDependencyCheck(context) 
* stageScanForSnyk(context, snykAuthenticationCode, buildFile)
* stageUploadToNexus(context)
* stageStartOpenshiftBuild(context)
* stageDeployToOpenshift(context)

## Workflow

The shared library does not impose which Git workflow you use. Whether you use git-flow, GitHub flow or a custom workflow, it is possible to configure the shared library according to your needs. There are just two settings to control everything: `branchToEnvironmentMapping` and `autoCloneEnvironmentsFromSourceMapping`.

### branchToEnvironmentMapping

Example:
```
branchToEnvironmentMapping: [
  "master": "prod",
  "develop": "dev",
  "hotfix/": "hotfix",
  "*": "review"
]
```

Maps a branch to an environment. There are three ways to reference branches:

* Fixed name (e.g. `master`)
* Prefix (ending with a slash, e.g. `hotfix/`)
* Any branch (`*`)

Matches are made top-to-bottom. For prefixes / any branch, a more specific environment might be selected if:

* the branch contains a ticket ID and a corresponding env exists in OCP. E.g. for mapping `"feature/": "dev"` and branch `feature/foo-123-bar`, the env `dev-123` is selected instead of `dev` if it exists.
* the branch name corresponds to an existing env in OCP. E.g. for mapping `"release/": "rel"` and branch `release/1.0.0`, the env `rel-1.0.0` is selected instead of `rel` if it exists.

### autoCloneEnvironmentsFromSourceMapping

Caution! Cloning environments on-the-fly is an advanced feature and should only be used if you understand OCP well, as there are many moving parts and things can go wrong in multiple places.

Example:
```
autoCloneEnvironmentsFromSourceMapping: [
  "hotfix": "prod",
  "review": "dev"
]
```

Instead of deploying multiple branches to the same environment, individual environments can be created on-the-fly. For example, the mapping `"*": "review"` deploys all branches to the `review` environment. To have one environment per branch / ticket ID, you can add the `review` environment to `autoCloneEnvironmentsFromSourceMapping`, e.g. like this: `"review": "dev"`. This will create individual environments (named e.g. `review-123` or `review-foobar`), each cloned from the `dev` environment.

### Examples

If you use [git-flow](https://jeffkreeftmeijer.com/git-flow/), the following config fits well:
```
branchToEnvironmentMapping: [
  'master': 'prod',
  'develop': 'dev',
  'release/': 'rel',
  'hotfix/': 'hotfix',
  '*': 'preview'
]
// Optionally, configure environments on-the-fly:
autoCloneEnvironmentsFromSourceMapping: [
  'rel': 'dev',
  'hotfix': 'prod',
  'preview': 'dev'
]
```

If you use [GitHub Flow](https://guides.github.com/introduction/flow/), the following config fits well:
```
branchToEnvironmentMapping: [
  'master': 'prod',
  '*': 'preview'
]
// Optionally, configure environments on-the-fly:
autoCloneEnvironmentsFromSourceMapping: [
  'preview': 'prod'
]
```

If you use a custom workflow, the config could look like this:
```
branchToEnvironmentMapping: [
  'production': 'prod',
  'master': 'dev',
  'staging': 'uat'
]
// Optionally, configure environments on-the-fly:
autoCloneEnvironmentsFromSourceMapping: [
  'uat': 'prod'
]
```


## Writing stages

Inside the closure passed to `odsPipeline`, you have full control. Write stages just like you would do in a normal `Jenkinsfile`. You have access to the `context`, which is assembled for you on the master node. The `context` can be influenced by changing the config map passed to `odsPipeline`. Please see `vars/odsPipeline.groovy` for possible options.

When you write stages, you have access to both global variables (defined without `def` in the `Jenkinsfile`) and the
`context` object. It contains the following properties:

| Property | Description |
| ------------- |-------------|
| jobName | Value of JOB_NAME. It is the name of the project of the build. |
| buildNumber | Value of BUILD_NUMBER. The current build number, such as "153". |
| buildUrl | Value of BUILD_URL. The URL where the results of the build can be found (e.g. http://buildserver/jenkins/job/MyJobName/123/) |
| buildTime | Time of the build, collected when the odsPipeline starts. |
| image | Container image to use for the Jenkins agent container. This value is not used when "podContainers" is set. |
| podLabel | Pod label, set by default to a random label to avoid caching issues. Set to a stable label if you want to reuse pods across builds. |
| podContainers | Custom pod containers to use. By default, only one container is used, and it is configure automatically. If you need to run multiple containers (e.g. app and database), then you can configure the containers via this property. |
| podVolumes | Volumes to make available to the pod. |
| podAlwaysPullImage | Determine whether to always pull the container image before each build run. |
| podServiceAccount | Serviceaccount to use when running the pod. |
| credentialsId | Credentials identifier (Credentials are created and named automatically by the OpenShift Jenkins plugin). |
| tagversion | The tagversion is made up of the build number and the first 8 chars of the commit SHA. |
| notifyNotGreen | Whether to send notifications if the build is not successful. |
| nexusHost | Nexus host (with scheme). |
| nexusUsername | Nexus username. |
| nexusPassword | Nexus password. |
| nexusHostWithBasicAuth | Nexus host (with scheme), including username and password as BasicAuth. |
| branchToEnvironmentMapping | Define which branches are deployed to which environments. |
| autoCloneEnvironmentsFromSourceMapping | Define which environments are cloned from which source environments. |
| cloneSourceEnv | The environment which was chosen as the clone source. |
| environment | The environment which was chosen as the deployment target, e.g. "dev". |
| targetProject | Target project, based on the environment. E.g. "foo-dev". |
| groupId | Group ID, defaults to: org.opendevstack.<projectID>. |
| projectId | Project ID, e.g. "foo". |
| componentId | Component ID, e.g. "be-auth-service". |
| gitUrl | Git URL of repository |
| gitBranch | Git branch for which the build runs. |
| gitCommit |Git commit SHA to build. |
| gitCommitAuthor | Git commit author. |
| gitCommitMessage | Git commit message. |
| gitCommitTime | Git commit time in RFC 3399. |
| sonarQubeBranch | Branch on which to run SonarQube analysis. |
| failOnSnykScanVulnerabilities | Boolean flag (default true) that disables build failure in case Snyk Scan founds vulnerabilities | 
| dependencyCheckBranch | Branch on which to run dependency checks. |
| environmentLimit | Number of environments to allow. |
| openshiftHost | OpenShift host - value taken from OPENSHIFT_API_URL. |
| odsSharedLibVersion | ODS Jenkins shared library version, taken from reference in Jenkinsfile. |
| bitbucketHost | BitBucket host - value taken from BITBUCKET_HOST. |
| environmentCreated | Whether an environment has been created during the build. |
| openshiftBuildTimeout | Timeout for the OpenShift build of the container image. |
| ciSkip | Whether the build should be skipped, based on the Git commit message. |


## Slave customization

The slave used to build your code can be customized by specifying the image to
use. Further, `podAlwaysPullImage` (defaulting to `true`) can be used to
determine whether this image should be refreshed on each build. The setting
`podVolumes` allows to mount persistent volume claims to the pod (the value is
passed to the `podTemplate` call as `volumes`). To control the container pods
completely, set `podContainers` (which is passed to the `podTemplate` call
as `containers`). See the
[kubernetes-plugin](https://github.com/jenkinsci/kubernetes-plugin)
documentation for possible configuration.


## Versioning

Each `Jenkinsfile` references a Git revsison of this library, e.g.
`library identifier: 'ods-library@production'`. The Git revsison can be a
branch (e.g. `production` or `0.1.x`), a tag (e.g.`0.1.1`) or a specific commit.

By default, each `Jenkinsfile` in `ods-project-quickstarters` on the `master`
branch references the `production` branch of this library. Quickstarters on a
branch point to the corresponding branch of the shared library - for example
a `Jenkinsfile` on branch `0.1.x` points to `0.1.x` of the shared library.

## Git LFS (Git Large File Storage extension)

If you are working with large files (e.g.: binary files, media files, files bigger than 5MB...),
you can follow the following steps:
- Check this HOWTO about [Git LFS](https://www.atlassian.com/git/tutorials/git-lfs)
- Track your large files in your local clone, as explained in previous step
- Enable Git LFS in your repository (if BitBucket: under repository's settings main page you can enable it)

**NOTE**: if already having a repository with large files and you want to migrate it to using git LFS:

```bash
git lfs migrate
```


## Development
* Try to write tests.
* See if you can split things up into classes.
* Keep in mind that you need to access e.g. `sh` via `script.sh`.


## Background
The implementation is largely based on https://www.relaxdiego.com/2018/02/jenkins-on-jenkins-shared-libraries.html. The scripted pipeline syntax was chosen because it is a better fit for a shared library. The declarative pipeline syntax is targeted for newcomers and/or simple pipelines (see https://jenkins.io/doc/book/pipeline/syntax/#scripted-pipeline). If you try to use it e.g. within a Groovy class you'll end up with lots of `script` blocks.
