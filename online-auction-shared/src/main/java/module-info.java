module online.auction.shared {
    requires com.google.gson;

    //Export
    exports com.auction.shared.message;
    exports com.auction.shared.model.auction;
    exports com.auction.shared.model.account;
    exports com.auction.shared.model.enums;
    exports com.auction.shared.model.base;
    exports com.auction.shared.model.item;
    exports com.auction.shared.model.payloads;
    exports com.auction.shared.util;
    exports com.auction.shared.model.dto;
    exports com.auction.shared.constant;

    //Opens
    opens com.auction.shared.message to com.google.gson;
    opens com.auction.shared.model.auction to com.google.gson;
    opens com.auction.shared.model.account to com.google.gson;
    opens com.auction.shared.model.enums to com.google.gson;
    opens com.auction.shared.model.base to com.google.gson;
    opens com.auction.shared.model.item to com.google.gson;
    opens com.auction.shared.model.payloads to com.google.gson;
    opens com.auction.shared.model.dto to com.google.gson;
    opens com.auction.shared.constant to com.google.gson;
}