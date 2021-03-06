/**
 *
 * StaffLeaveDetector.java
 * ZhangMin.name - zhangmin@zhangmin.name
 * org.ciotc.middleware.adapter.positioning
 *
 */
package org.ciotc.middleware.adapter.positioning.util;

import java.util.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ciotc.middleware.adapter.positioning.pojo.TracingTargetDto;
import org.ciotc.middleware.notification.StaffMessageDto;


/**
 * @author ZhangMin.name
 *
 */
public class StaffLeaveDetector {
	private static Map<String,StaffMessageDto> tracingTargets;
	private static final Log logingData = LogFactory.getLog("positiondata");
	private static final Log logger = LogFactory.getLog(StaffLeaveDetector.class);
	private StaffAlertDAO staffAlertDAO;
	StaffLeaveDetector(StaffAlertDAO staffAlertDAO){
		this.staffAlertDAO = staffAlertDAO;
		refresh();
	}
	public void setStaffAlertDAO(StaffAlertDAO staffAlertDAO){
		this.staffAlertDAO = staffAlertDAO;
	}
    private void refresh(){
    	List<TracingTargetDto> ttds = 
    			staffAlertDAO.getTracingTargetsByLBSTraceTable();
    	Iterator<TracingTargetDto> it = ttds.iterator();
    	tracingTargets = new HashMap<String,StaffMessageDto>();
    	while(it.hasNext()){
    		TracingTargetDto ttd = it.next();
    		StaffMessageDto smd = new StaffMessageDto();
    		smd.setCardID(ttd.getTargetID());
    		smd.setTime(StaffAlertDAOImpl.tsToString(ttd.getElTime()));
    		tracingTargets.put(ttd.getTargetID(), smd);
    	}
    	System.out.println("Refresh Map finished.");
    }
	public static void put(StaffMessageDto smd){
		synchronized(tracingTargets){
			
			tracingTargets.put(smd.getCardID(), smd);
		}
	}

	public void runAlertJob() {
		logger.info("Staff Leave detector timer task started.");
		//refresh();
		Map <String,StaffMessageDto> targets = tracingTargets;
		Set<String> keys = targets.keySet();
		Iterator<String> it = keys.iterator();
		while(it.hasNext()){
			String cardID = it.next();
			StaffMessageDto smd = targets.get(cardID);
			Timestamp ts = Timestamp.valueOf(smd.getTime());
			long last = ts.getTime();
			long now = System.currentTimeMillis();
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(last);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String tts = sdf.format(c.getTime());
			if((now - last) > 60 * 1000){
				//Leaving
				logingData.info(cardID + " has left,time:" + tts + ",now:" 
						+ sdf.format(new Date()));
				staffAlertDAO.updateEnterLeaveInfo(smd);
				it.remove();
			}
		}
				
	}
	public static void main(String[] args){
		
	}

}
