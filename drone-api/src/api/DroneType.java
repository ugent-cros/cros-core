package models;


/**
 * Created by yasser on 17/03/15.
 */
public class DroneType {

    private String type;
    private String versionNumber;

    public DroneType(String type, String versionNumber) {
        this.type = type;
        this.versionNumber = versionNumber;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;

        if (o != null && o instanceof DroneType) {
            DroneType other = (DroneType) o;

            boolean sameType = (type == null && other.getType() == null) || (type != null && type.equals(other.getType()));
            boolean sameVersion = (versionNumber == null && other.getVersionNumber() == null) || (versionNumber != null && versionNumber.equals(other.getVersionNumber()));
            return sameType && sameVersion;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 57;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (versionNumber != null ? versionNumber.hashCode() : 0);
        return result;
    }
}
