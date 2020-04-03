package enklikanketa.com.a1kapanel.Models.SurveyList;

import android.os.Parcel;
import android.os.Parcelable;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;

public class Survey implements Parcelable {
    public String litem_zgoraj = null;
    public String litem_spodaj = null;

    public String srv_id = null;
    public String link = null;
    public String status = null;
    public String srv_version = null;
    public String version_datetime = null;
    public String tgeof_id = null;
    public String tact_id = null;
    public String mode = null;
    public String latitude = null;
    public String longitude = null;
    public String user_id = null;
    public String timestamp = null;

    public long item_id;

    public Survey() {
        item_id = GeneralLib.getRandomInt();
    }

    public static final Parcelable.Creator<Survey> CREATOR = new Parcelable.Creator<Survey>() {
        public Survey createFromParcel(Parcel in) {
            return new Survey();
        }
        public Survey[] newArray(int size) {
            return new Survey[size];
        }

    };
    public Survey(Parcel in) {
        this.item_id = in.readLong();
        this.litem_zgoraj = in.readString();
        this.litem_spodaj = in.readString();
        this.srv_id = in.readString();
        this.link = in.readString();
        this.status = in.readString();
        this.srv_version = in.readString();
        this.version_datetime = in.readString();
        this.tgeof_id = in.readString();
        this.tact_id = in.readString();
        this.mode = in.readString();
        this.latitude = in.readString();
        this.longitude = in.readString();
        this.user_id = in.readString();
        this.timestamp = in.readString();
    }
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.item_id);
        dest.writeString(this.litem_zgoraj);
        dest.writeString(this.litem_spodaj);
        dest.writeString(this.srv_id);
        dest.writeString(this.link);
        dest.writeString(this.status);
        dest.writeString(this.srv_version);
        dest.writeString(this.version_datetime);
        dest.writeString(this.tgeof_id);
        dest.writeString(this.tact_id);
        dest.writeString(this.mode);
        dest.writeString(this.latitude);
        dest.writeString(this.longitude);
        dest.writeString(this.user_id);
        dest.writeString(this.timestamp);
    }
    public void setListItemTop(String litem_zgoraj){
        this.litem_zgoraj = litem_zgoraj;
    }
    public void setListItemBottom(String litem_spodaj){
        this.litem_spodaj = litem_spodaj;
    }
    public void setLink(String link){
        this.link = link;
    }
    public void setStatus(String status){
        this.status = status;
    }
    public void setSrvId(String srv_id){
        this.srv_id = srv_id;
    }
    public void setVerDatetime(String version_datetime){
        this.version_datetime = version_datetime;
    }
    public void setTGeoId(String tgeof_id){
        this.tgeof_id = tgeof_id;
    }
    public void setTActId(String tact_id){
        this.tact_id = tact_id;
    }
    public void setMode(String mode){
        this.mode = mode;
    }
    public void setLatitude(String latitude){
        this.latitude = latitude;
    }
    public void setLongitude(String longitude){
        this.longitude = longitude;
    }
    public void setSrvVersion(String srv_version){
        this.srv_version = srv_version;
    }
    public void setUserId(String user_id){
        this.user_id = user_id;
    }
    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }

    public String getMode(){
        return this.mode;
    }
    public String getUserId(){
        return this.user_id;
    }
    public String getSrvId(){
        return this.srv_id;
    }
    public String getZgoraj(){
        return this.litem_zgoraj;
    }
    public String getSpodaj(){
        return this.litem_spodaj;
    }
    public String getTimestamp(){
        return this.timestamp;
    }

    @Override
    public String toString(){
        return "litem_zgoraj="+litem_zgoraj+" litem_spodaj="+litem_spodaj+" srv_id="+srv_id+
                " link="+link+" status="+status+" srv_version="+srv_version+" version_datetime="+
                version_datetime+" tgeof_id="+tgeof_id+" user_id="+user_id+" timestamp="+timestamp
                +" tact_id="+tact_id+" mode="+mode+" latitude="+latitude+" longitude="+longitude;
    }
}
