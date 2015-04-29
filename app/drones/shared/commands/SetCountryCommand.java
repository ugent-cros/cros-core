package drones.shared.commands;

import java.io.Serializable;

/**
 * Created by Cedric on 3/20/2015.
 */
public class SetCountryCommand implements Serializable {
    private String country;

    /**
     * Creates a new country command
     * @param country Country code with ISO 3166 format
     */
    public SetCountryCommand(String country) {
        if(country.length() != 2)
            throw new IllegalArgumentException("country should be in ISO 3166-2 format");
        this.country = country;
    }

    public String getCountry() {
        return country;
    }
}
