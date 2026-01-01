package com.fluffycat.sentinelapp.common.env;

import java.util.function.Supplier;

public class DefaultEnvResolver implements EnvResolver{
    private final Supplier<String> defaultEnvSupplier;
    public DefaultEnvResolver(Supplier<String> defaultEnvSupplier){
        this.defaultEnvSupplier = defaultEnvSupplier;
    }
    @Override
    public String resolve(String requestEnv) {
        if (requestEnv!=null && !requestEnv.isBlank()){
            return requestEnv.trim();
        }
        String cfg =  defaultEnvSupplier.get();
        if(cfg!=null && !cfg.isBlank()){
            return cfg.trim();
        }
        return "local";
    }
}
