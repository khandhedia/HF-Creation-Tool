package com.rnd.hftool.enums;

/**
 * Created by nirk0816 on 5/26/2017.
 */
public enum FileTokens
{
    TOKEN_MODULE("Module"),
    TOKEN_BASE("Base"),
    TOKEN_COMPONENT("Component"),
    UNKNOWN("Unknown");

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
        if (token.equalsIgnoreCase("module")) { return TOKEN_MODULE; }
        else if (token.equalsIgnoreCase("base")) { return TOKEN_BASE; }
        else if (token.equalsIgnoreCase("component")) { return TOKEN_COMPONENT; }
        else { return UNKNOWN; }
    }


}
