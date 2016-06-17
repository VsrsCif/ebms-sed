/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui.entities;

import java.util.Date;

/**
 *
 * @author sluzba
 */
public class CronExecutionFilter {

    protected String name;
    protected Date startTimestampFrom;
    protected Date startTimestampTo;
    protected String status;

    protected String type;

    public String getName() {
        return name;
    }

    public Date getStartTimestampFrom() {
        return startTimestampFrom;
    }

    public Date getStartTimestampTo() {
        return startTimestampTo;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartTimestampFrom(Date startTimestampFrom) {
        this.startTimestampFrom = startTimestampFrom;
    }

    public void setStartTimestampTo(Date startTimestampTo) {
        this.startTimestampTo = startTimestampTo;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setType(String type) {
        this.type = type;
    }

}
