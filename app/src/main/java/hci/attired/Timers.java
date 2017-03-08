package hci.attired;

import java.util.Date;

/**
 * Created by Naeem on 07/03/2017.
 */

public class Timers {
    private Date lastUpdate;

    public Timers(Date last_Update){
        this.lastUpdate = last_Update;
    }

    public Date getLastUpdate(){
        return this.lastUpdate;
    }

    public void setLastUpdate(Date curDate){
        this.lastUpdate = curDate;
    }
}
