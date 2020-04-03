package enklikanketa.com.a1kapanel.Models.StudyList;

import java.io.Serializable;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;

public class Study implements Serializable {
    public String litem_top = null;
    public String litem_bottom = null;

    public String srv_id = null;
    public String link = null;
    public String active = null;
    public String title = null;
    public String subscribed = null;
    public String start = null;
    public String end = null;
    public String unfinished_cnt = null;
    public String description = null;
    public boolean location_permission = false;
    public boolean ar_permission = false;
    public String unixstart = null;

    public long item_id;

    public Study() {
        item_id = GeneralLib.getRandomInt();
}

    public void setListItemTop(String litem_top) {
        this.litem_top = litem_top;
    }

    public void setListItemBottom(String litem_bottom) {
        this.litem_bottom = litem_bottom;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public void setSrvId(String srv_id) {
        this.srv_id = srv_id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubscribed(String subscribed) {
        this.subscribed = subscribed;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setEnd(String start) {
        this.start = start;
    }

    public void setUnfinishedCnt(String unfinished_cnt) {
        this.unfinished_cnt = unfinished_cnt;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUnixstart(String unixstart) {
        this.unixstart = unixstart;
    }

    public void setLocationPermission(boolean location_permission) {
        this.location_permission = location_permission;
    }

    public void setARPermission(boolean ar_permission) {
        this.ar_permission = ar_permission;
    }

    public String getActive() {
        return this.active;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSrvId() {
        return this.srv_id;
    }

    public String getZgoraj() {
        return this.litem_top;
    }

    public String getSpodaj() {
        return this.litem_bottom;
    }

    public String getStart() {
        return this.start;
    }

    public String getEnd() {
        return this.end;
    }

    public String getLink() {
        return this.link;
    }

    public String getUnfinishedCnt() {
        return this.unfinished_cnt;
    }

    public String getDescription() {
        return this.description;
    }

    public String getUnixstart() {
        return this.unixstart;
    }

    public boolean getLocationPermission() {
        return this.location_permission;
    }

    public boolean getARPermission() {
        return this.ar_permission;
    }

    public String toString() {
        return "litem_zgoraj=" + litem_top + " litem_spodaj=" + litem_bottom + " srv_id=" + srv_id +
                " link=" + link + " active=" + active + " title=" + title + " subscribed=" +
                subscribed + " start=" + start + " end=" + end + " description=" + description +
                " loc_perm=" + location_permission + " ar_perm=" + ar_permission;
    }
}

