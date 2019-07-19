package com.htc.wallet.skrsdk.drives.onedrive;

public class Constants {
    public static final String AUTHORITY_URL = "https://login.microsoftonline.com/common";
    public static final String AUTHORIZATION_ENDPOINT = "/oauth2/v2.0/authorize";
    public static final String TOKEN_ENDPOINT = "/oauth2/v2.0/token";
    // The Microsoft Graph delegated permissions that you set in the application
    // registration portal must match these scope values.
    // Update this constant with the scope (permission) values for your application
    public static final String[] SCOPES = {
            "openid",
            "User.Read",
            "Files.ReadWrite.All",
            "Files.ReadWrite.AppFolder",
            "Sites.ReadWrite.All"
    };
}
