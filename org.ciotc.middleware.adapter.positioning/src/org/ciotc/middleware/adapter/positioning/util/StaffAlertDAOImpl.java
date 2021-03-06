/**
 *
 * StaffAlertDAOImpl.java
 * ZhangMin.name - zhangmin@zhangmin.name
 * org.ciotc.middleware.threadedtimertask
 *
 */
package org.ciotc.middleware.adapter.positioning.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ciotc.middleware.adapter.positioning.pojo.TargetInfoDto;
import org.ciotc.middleware.adapter.positioning.pojo.TracingTargetDto;
import org.ciotc.middleware.adapter.positioning.pojo.UserTargetOrgnaizeDto;
import org.ciotc.middleware.notification.StaffMessageDto;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * 数据库操作封装类
 * @author ZhangMin.name
 *
 */
public class StaffAlertDAOImpl implements StaffAlertDAO{
	private static final Log logger = LogFactory.getLog(StaffAlertDAOImpl.class);
	protected SimpleDriverDataSource dataSource;
	public void setDataSource(SimpleDriverDataSource sdds){
		this.dataSource = sdds;
	}
	public ResultSet exeuteSQL(String sql){
		Connection conn = null;
		ResultSet rs = null;
		if(dataSource == null){
			logger.error("dataSource can not be null");
		}
		try {
			conn = dataSource.getConnection();
			Statement stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("execute sql error " + sql);
		}finally{
			close(conn);
		}
		return rs;
	}
	public Connection getConnection(){
		Connection conn = null;
		if(dataSource == null){
			logger.error("dataSource can not be null");
		}
		try {
			 conn = dataSource.getConnection();
		} catch (SQLException e) {
			logger.error("Can not get connection from dataSource");
		}
		return conn;
	}
	public void close(Connection conn){
		try {
			if (conn != null) {
				conn.close();
				conn = null;
			}
		} catch (SQLException e) {
			logger.error("close SQLException!");
			e.printStackTrace();
		}
	}
	/**
	 * 向t_manageeventlog表中插入告警事件，如果已经被处理则取消插入。
	 */
	@Override
	public void insertEventLog(int eventTypeID, int subEventType, String targetID,
			int userID) {
		Connection conn = getConnection();
		Date date = new Date();
		Timestamp eventTime = new Timestamp(date.getTime());
		try {
			Statement statement = conn.createStatement();
			String sql = 
					"SELECT handlestatus FROM t_manageeventlog WHERE eventtype_id=" 
					+ eventTypeID 
					+ " AND subEventType=" + subEventType + " AND user_id=" 
					+ userID + " AND handlestatus=0";
			ResultSet rs = statement.executeQuery(sql);
			if(!rs.next()) {
				String insertSql = 
						"INSERT INTO t_manageeventlog(event_time, "
						+ "eventtype_id, subevent_type, target_id, user_id) " 
						+ "VALUES (?, ?, ?, ?, ?)";
				PreparedStatement ps = conn.prepareStatement(insertSql);
				ps.setTimestamp(1, eventTime);
				ps.setInt(2, eventTypeID);
				ps.setInt(3, subEventType);
				ps.setString(4, targetID);
				ps.setInt(5, userID);
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			logger.error("SQLException: " + e.getMessage());
		} finally{
			close(conn);
		}
	}
	/**
	 * 从实时定位表t_lbstracedata中查询正在被定位的target_id
	 */
	@Override
	public List<String> getTargetsFromLBSTraceTable() {
		List<String> targets = new ArrayList<String>();
		ResultSet rs = this.exeuteSQL(
				"SELECT target_id FROM t_lbstracedata");
		try {
			while(rs.next()){
				targets.add(rs.getString(1));
			}
		} catch (SQLException e) {
			logger.error("error occured when executing sql");
			e.printStackTrace();
		}
		return targets;
	}
	/**
	 * 根据target_id获取Map<target_id,user_id>
	 */
	@Override
	public Map<String, Integer> getTargetUserByTargetID(
			List<String> targets) {
		Connection conn = this.getConnection();
		Map<String,Integer> targetToUser = new HashMap<String,Integer>();
		Iterator<String> it = targets.iterator();
		while(it.hasNext()){
			String target = it.next();
			try {
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(
						"SELECT target_id,user_id FROM T_UserTargetOrgnaize " +
						" Where target_id = \'" + target + "\'");
				while(rs.next()){
					targetToUser.put(rs.getString(1), rs.getInt(2));
				}
				
			} catch (SQLException e) {
				logger.error("error occured when executing sql");
				e.printStackTrace();
			}
			
		}
		close(conn);
		return targetToUser;
	}
	/**
	 * 根据电池类型获得电池估计寿命
	 */
	@Override
	public int getBatteryLifeByID(String battery) {
		int avgtime = -1;
		ResultSet rs = this.exeuteSQL(
				"SELECT avgtime FROM t_battery WHERE battery_id = \'"
				+ battery + "\'");
		try {
			rs.next();
			avgtime = rs.getInt(1);
		} catch (SQLException e) {
			logger.error("error occured when executing sql");
			e.printStackTrace();
		}
		return avgtime;
	}
	/**
	 * 根据实时定位表中的target_id获取Target对象
	 */
	@Override
	public List<TargetInfoDto> getTargetsInfoByLBSTraceTable() {
		List<TargetInfoDto> targets = new ArrayList<TargetInfoDto>();
		ResultSet rs = this.exeuteSQL(
				"SELECT * FROM t_target WHERE target_id "+
				"IN (SELECT DISTINCT target_id FROM t_lbstracedata)");
		try {
			while(rs.next()){
				TargetInfoDto ttd = new TargetInfoDto();
				ttd.setTargetID(rs.getString("target_id"));
				ttd.setTargetUseTime(rs.getInt("target_usetime"));
				ttd.setBattery(rs.getString("battery_id"));
				ttd.setTargetStatus(rs.getInt("target_status"));
				ttd.setTargetBarCode(rs.getString("target_barcode"));
				ttd.setRemark(rs.getString("remark"));
				targets.add(ttd);
			}
		} catch (SQLException e) {
			logger.error("error occured when executing sql");
			e.printStackTrace();
		}
		return targets;
	}
	/**
	 * 获取正在被定为对象的UserTargetOrganize属性封装类
	 */
	@Override
	public List<UserTargetOrgnaizeDto> getUTOByLBSTraceTable() {
		List<UserTargetOrgnaizeDto> utos = 
				new ArrayList<UserTargetOrgnaizeDto>();
		ResultSet rs = this.exeuteSQL(
				"SELECT * FROM T_UserTargetOrgnaize" +
				" Where target_id IN " +
			    "(SELECT DISTINCT target_id FROM T_LBSTraceData)");
		try {
			while(rs.next()){
				UserTargetOrgnaizeDto uto = new UserTargetOrgnaizeDto();
				uto.setUserID(rs.getInt("user_id"));
				uto.setOrganizeID(rs.getInt("organize_id"));
				uto.setTargetID(rs.getString("target_id"));
				uto.setTargetCode(rs.getString("target_code"));
				uto.setValidDate(rs.getTimestamp("validdate"));
				uto.setDistributeStatus(rs.getInt("distributestatue"));
				uto.setDistributeTime(rs.getTimestamp("distributetime"));
				uto.setRecycleTime(rs.getTimestamp("recycletime"));
				uto.setRecycleStatus(rs.getInt("recyclestatue"));
				uto.setVersion(rs.getInt("version"));
				uto.setReason(rs.getString("reason"));
				uto.setOperaterID(rs.getInt("operater_id"));
				uto.setUsertypeID(rs.getInt("usertype_id"));
				utos.add(uto);
			}
		} catch (SQLException e) {
			logger.error("error occured when executing sql");
			e.printStackTrace();
		}
		return utos;
	}
	/**
	 * 根据target_id和user_id 及事件类型插入告警数据库
	 */
	@Override
	public void alarm(int eventTypeID, int subEventType,
			Map<String, Integer> targetToUsers) {
		Set<String> users = targetToUsers.keySet();
		Iterator<String> it2 = users.iterator();
		while(it2.hasNext()){
			String target = it2.next();
			int user = targetToUsers.get(target);
			logger.info("Insert AlertEvent: " + "Type:" + 
						eventTypeID + ",SubType:" +
						subEventType + ",user_id:" + 
						user + ",target_id:" + target);
			//TODO 暂时不插入数据库，方便测试
			//this.insertEventLog(eventTypeID, subEventType, target, user);
		}
		
	}
	/**
	 * 从t_lbstracedata中获取当前正在被追踪定位的对象
	 */
	@Override
	public List<TracingTargetDto> getTracingTargetsByLBSTraceTable() {
		List<TracingTargetDto> tts = new ArrayList<TracingTargetDto>();
		ResultSet rs = this.exeuteSQL(
				"SELECT * FROM t_lbstracedata");
		try {
			while(rs.next()){
				TracingTargetDto tt = new TracingTargetDto();
				tt.setAreaID(rs.getInt("area_id"));
				tt.setElFlag(rs.getInt("elflag"));
				tt.setElTime(rs.getTimestamp("eltime"));
				tt.setTargetID(rs.getString("target_id"));
				tt.setUserID(rs.getInt("user_id"));
				tts.add(tt);
			}
		} catch (SQLException e) {
			logger.error("error occured when executing sql");
			e.printStackTrace();
		}
		return tts;
	}
	/**
	 * 根据antenna_id 和进去标志elflag查询当前定位对象中在要离开的定位对象
	 */
	@Override
	public List<TracingTargetDto> getLeavingTracingTargetByAntennaID(
			String antennaID) {
		List<TracingTargetDto> tts = new ArrayList<TracingTargetDto>();
		ResultSet rs = this.exeuteSQL(
				"SELECT * FROM t_lbstracedata WHERE target_id " +
				"IN ( SELECT target_id FROM t_lbsdata WHERE elflag = 1 AND " +
				" antenna_id = " + antennaID + " )");
		try {
			while(rs.next()){
				TracingTargetDto tt = new TracingTargetDto();
				tt.setAreaID(rs.getInt("area_id"));
				tt.setElFlag(rs.getInt("elflag"));
				tt.setElTime(rs.getTimestamp("eltime"));
				tt.setTargetID(rs.getString("target_id"));
				tt.setUserID(rs.getInt("user_id"));
				tts.add(tt);
			}
		} catch (SQLException e) {
			logger.error("error occured when executing sql");
			e.printStackTrace();
		}
		return tts;
	}
	/**
	 * 根据设备类型从表t_antenna中获取antenna_id
	 */
	@Override
	public String getAntennaIDByDevice(int deviceType) {
		String antenna = null;
		ResultSet rs = this.exeuteSQL(
				"SELECT antenna_id FROM t_antenna WHERE devicetype_id = " 
						+ deviceType);
		try {
			rs.next();
			antenna = rs.getString(1);
		} catch (SQLException e) {
			logger.error("error occured when executing sql");
			e.printStackTrace();
		}
		
		return antenna;
	}
	/**
	 * 当检测到有人员离开通讯基站可以覆盖的范围时，更新
	 * t_enterleaveinfo表，并删除t_lbstracedata表
	 * 中的相关条目。！作为一个事务执行。
	 */
	@Override
	public void updateEnterLeaveInfo(StaffMessageDto smd) {
		Connection conn = this.getConnection();
		
		
		try {
			Statement stmt1 = conn.createStatement();
			ResultSet rs1 = stmt1.executeQuery(
					"SELECT target_id,eltype FROM t_enterleaveinfo WHERE target_id = \'" 
							+ smd.getCardID() + "\' ORDER BY eltime DESC LIMIT 1");
		
			while(rs1.next()){
			conn.setAutoCommit(false);
			if(rs1.getInt("eltype") == 0){
				String targetID = smd.getCardID();
				Statement stmt = conn.createStatement();
				UserTargetOrgnaizeDto uto = this.getUTOByTargetID(targetID);
				StringBuffer sql = new StringBuffer();
				sql.append(
						"INSERT INTO t_enterleaveinfo(user_id,organize_id,target_id,");
				sql.append("target_code,validdate,distributestatue,distributetime,");
				sql.append("recyclestatue,recycletime,eltype,eltime)VALUES( " );
				sql.append(uto.getUserID()).append(",");
				sql.append(uto.getOrganizeID()).append(",");
				sql.append(uto.getTargetID()).append(",");
				sql.append(uto.getTargetCode()).append(",");
				sql.append(sT(uto.getValidDate())).append(",");
				sql.append(uto.getDistributeStatus()).append(",");
				sql.append(sT(uto.getDistributeTime())).append(",");
				sql.append(uto.getRecycleStatus()).append(",");
				sql.append(sT(uto.getRecycleTime())).append(",");
				sql.append("1").append(",\'").append(smd.getTime());
				sql.append("\')");
				//System.out.println("SQL :" + sql.toString());
				int status = stmt.executeUpdate(sql.toString());
				if(status == 1){
	        	stmt.executeUpdate(
	        			"DELETE FROM t_lbstracedata WHERE target_id = \'" +
	        	        smd.getCardID() +"\'");
				}
			}else{
				stmt1.executeUpdate(
        			"DELETE FROM t_lbstracedata WHERE target_id = \'" +
        	        smd.getCardID() +"\'");
			}
			conn.commit();
			} 		
		} catch (SQLException e) {
			logger.error("error occured when executing sql");
			e.printStackTrace();
		}
		close(conn);
		
	}
	/**
	 * 将一个Timestamp对象格式化为 'yyyy-MM-dd HH:mm:ss',
	 * 用于数据中数据的插入
	 * 因为Timestamp自带方法均已被废弃
	 * @param ts
	 * @return
	 */
	public String sT(Timestamp ts){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(ts.getTime());
		StringBuffer sb = new StringBuffer();
		sb.append("\'").append(sdf.format(c.getTime())).append("\'");
		return sb.toString();
	}
	/**
	 * 将一个Timestamp对象格式化为 yyyy-MM-dd HH:mm:ss,
	 * 因为Timestamp自带方法均已被废弃
	 * @param ts
	 * @return
	 */
	public static String tsToString(Timestamp ts){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(ts.getTime());
		StringBuffer sb = new StringBuffer();
		sb.append(sdf.format(c.getTime()));
		return sb.toString();
	}
	/*
	 * 根据target_id 获取UserTargetOrganizeDto
	 */
	@Override
	public UserTargetOrgnaizeDto getUTOByTargetID(String targetID) {
		UserTargetOrgnaizeDto uto = new UserTargetOrgnaizeDto();
		ResultSet rs = this.exeuteSQL(
				"SELECT * FROM T_UserTargetOrgnaize" +
				" Where target_id = \'" + targetID + "\'");
		try {
			while(rs.next()){
				uto.setUserID(rs.getInt("user_id"));
				uto.setOrganizeID(rs.getInt("organize_id"));
				uto.setTargetID(rs.getString("target_id"));
				uto.setTargetCode(rs.getString("target_code"));
				uto.setValidDate(rs.getTimestamp("validdate"));
				uto.setDistributeStatus(rs.getInt("distributestatue"));
				uto.setDistributeTime(rs.getTimestamp("distributetime"));
				uto.setRecycleTime(rs.getTimestamp("recycletime"));
				uto.setRecycleStatus(rs.getInt("recyclestatue"));
				uto.setVersion(rs.getInt("version"));
				uto.setReason(rs.getString("reason"));
				uto.setOperaterID(rs.getInt("operater_id"));
				uto.setUsertypeID(rs.getInt("usertype_id"));
			}
		} catch (SQLException e) {
			logger.error("error occured when executing sql");
			e.printStackTrace();
		}
		return uto;
	}
	/*
	 * 根据List<target_id> 获取UserTargetOrganizeDto
	 */
	@Override
	public List<UserTargetOrgnaizeDto> getUTOByTargetIDs(List<String> targetIDs) {
		List<UserTargetOrgnaizeDto> utos = 
				new ArrayList<UserTargetOrgnaizeDto>();
		Connection conn = this.getConnection();
		Iterator<String> it = targetIDs.iterator();
		try {
			PreparedStatement ps = conn.prepareStatement(
					"SELECT * FROM T_UserTargetOrgnaize" +
					" Where target_id = ? ");
			while(it.hasNext()){
				String targetID = it.next();
				ps.setString(1, targetID);
				ResultSet rs = ps.executeQuery();
				while(rs.next()){
					UserTargetOrgnaizeDto uto = new UserTargetOrgnaizeDto();
					uto.setUserID(rs.getInt("user_id"));
					uto.setOrganizeID(rs.getInt("organize_id"));
					uto.setTargetID(rs.getString("target_id"));
					uto.setTargetCode(rs.getString("target_code"));
					uto.setValidDate(rs.getTimestamp("validdate"));
					uto.setDistributeStatus(rs.getInt("distributestatue"));
					uto.setDistributeTime(rs.getTimestamp("distributetime"));
					uto.setRecycleTime(rs.getTimestamp("recycletime"));
					uto.setRecycleStatus(rs.getInt("recyclestatue"));
					uto.setVersion(rs.getInt("version"));
					uto.setReason(rs.getString("reason"));
					uto.setOperaterID(rs.getInt("operater_id"));
					uto.setUsertypeID(rs.getInt("usertype_id"));
					utos.add(uto);
				}
			}
		} catch (SQLException e) {
			logger.error("error occured when executing sql");
			e.printStackTrace();
		}
		return utos;
	}
}
