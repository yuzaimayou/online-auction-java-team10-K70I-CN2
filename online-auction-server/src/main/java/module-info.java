module online.auction.server {
    requires online.auction.shared;

    requires com.google.gson;
    requires java.sql;
    requires jdk.httpserver;
    requires jdk.compiler;
    requires jakarta.mail;
    requires io.github.cdimascio.dotenv.java;
    requires com.zaxxer.hikari;
    requires java.net.http;
    requires java.xml.crypto;

    opens com.auction.server.model to com.google.gson;
}