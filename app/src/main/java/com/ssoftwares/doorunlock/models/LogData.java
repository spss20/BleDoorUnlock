package com.ssoftwares.doorunlock.models;

import com.google.gson.annotations.SerializedName;

public class LogData {
    @SerializedName("mac")
    private String mac;
    @SerializedName("user")
    private String user;
    @SerializedName("board")
    private String board;
    @SerializedName("gate_status")
    private String gateStatus;
    @SerializedName("datetime")
    private String datetime;
    @SerializedName("open_method")
    private String openMethod;

    public LogData(){

    }
    public LogData(String mac, String user, String board, String gateStatus, String datetime, String openMethod) {
        this.mac = mac;
        this.user = user;
        this.board = board;
        this.gateStatus = gateStatus;
        this.datetime = datetime;
        this.openMethod = openMethod;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getGateStatus() {
        return gateStatus;
    }

    public void setGateStatus(String gateStatus) {
        this.gateStatus = gateStatus;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getOpenMethod() {
        return openMethod;
    }

    public void setOpenMethod(String openMethod) {
        this.openMethod = openMethod;
    }
}
