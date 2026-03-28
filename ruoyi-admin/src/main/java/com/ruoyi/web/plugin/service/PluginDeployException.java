package com.ruoyi.web.plugin.service;

public class PluginDeployException extends Exception {
    private String step;

    public PluginDeployException(String message) {
        super(message);
    }

    public PluginDeployException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginDeployException(String step, String message, Throwable cause) {
        super(message, cause);
        this.step = step;
    }

    public String getStep() {
        return step;
    }
}
