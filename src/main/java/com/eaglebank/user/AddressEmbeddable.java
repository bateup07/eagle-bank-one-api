package com.eaglebank.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Provides support for address embeddable.
 *
 * @author mattbateup
 */
@Embeddable
public class AddressEmbeddable {

    @Column(nullable = false, length = 255)
    private String line1;

    @Column(length = 255)
    private String line2;

    @Column(length = 255)
    private String line3;

    @Column(nullable = false, length = 100)
    private String town;

    @Column(nullable = false, length = 100)
    private String county;

    @Column(nullable = false, length = 32)
    private String postcode;

    protected AddressEmbeddable() {
    }

    public AddressEmbeddable(String line1, String line2, String line3, String town, String county, String postcode) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.town = town;
        this.county = county;
        this.postcode = postcode;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getLine3() {
        return line3;
    }

    public String getTown() {
        return town;
    }

    public String getCounty() {
        return county;
    }

    public String getPostcode() {
        return postcode;
    }
}
