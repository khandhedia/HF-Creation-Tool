package com.rnd.hftool.enums;

/**
 * Created by nirk0816 on 5/26/2017.
 */
public enum FileTokens
{

    MODULE("module"),
    BASE("base"),
    COMPONENT("component"),
    UNKNOWN("unknown");

    String token;

    FileTokens(String token)
    {
        this.token = token;
    }

    public String getToken()
    {
        return token;
    }

    public static FileTokens createToken(String token)
    {
        if(token.equalsIgnoreCase("module"))
            return FileTokens.MODULE;
        else if(token.equalsIgnoreCase("base"))
            return FileTokens.BASE;
        else if(token.equalsIgnoreCase("component"))
            return FileTokens.COMPONENT;
        else
            return FileTokens.UNKNOWN;
    }

}
