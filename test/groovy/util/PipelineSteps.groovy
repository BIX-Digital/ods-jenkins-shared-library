package util

import org.ods.orchestration.util.IPipelineSteps

class PipelineSteps implements IPipelineSteps {

    private Map currentBuild = [:]
    private Map env = [:]

    PipelineSteps() {
        env.WORKSPACE = System.getProperty("java.io.tmpdir")
    }

    void archiveArtifacts(String artifacts) {
    }

    def checkout(Map config) {
        return [:]
    }

    void dir(String path, Closure block) {
        block()
    }

    void echo(String message) {
    }

    def getCurrentBuild() {
        return currentBuild
    }

    Map getEnv() {
        return env
    }

    void junit(String path) {
    }

    def load(String path) {
        return [:]
    }

    def sh(def args) {
        return ""
    }

    void stage(String name, Closure block) {
        block()
    }

    void stash(String name) {
    }

    void unstash(String name) {
    }

    @Override
    def fileExists(String file) {
        return null
    }

    @Override
    def readFile(String file, String encoding) {
        return null
    }

    @Override
    def readFile(Map args) {
        return null
    }

    @Override
    def writeFile(String file, String text, String encoding) {
        return null
    }

    @Override
    def writeFile(Map args) {
        return null
    }

    @Override
    def readJSON(Map args) {
        return null
    }

    @Override
    def writeJSON(Map args) {
        return null
    }

    @Override
    def timeout(Map args, Closure block) {
        return null
    }

    @Override
    def deleteDir() {
        return null
    }
    
    @Override
    def withEnv(java.util.List env, groovy.lang.Closure block) {
      block()
    }
    
    @Override
    def unstable (String message) {
    }
}
