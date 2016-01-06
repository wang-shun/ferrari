package com.cip.ferrari.admin.controller;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cip.ferrari.admin.core.model.ReturnT;
import com.cip.ferrari.admin.core.model.FerrariJobLog;
import com.cip.ferrari.admin.core.util.HttpUtil;
import com.cip.ferrari.admin.core.util.JacksonUtil;
import com.cip.ferrari.admin.dao.IFerrariJobLogDao;
import com.cip.ferrari.core.job.result.FerrariFeedback;

/**
 * index controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/joblog")
public class JobLogController {
	private static Logger Logger = LoggerFactory.getLogger(JobLogController.class);
	
	@Resource
	public IFerrariJobLogDao ferraliJobLogDao;
	
	@RequestMapping("/save")
	@ResponseBody
	public ReturnT<String> triggerLog(int triggerLogId, String status, String msg) {
		FerrariJobLog log = ferraliJobLogDao.load(triggerLogId);
		if (log!=null) {
			log.setHandleTime(new Date());
			log.setHandleStatus(status);
			log.setHandleMsg(msg);
			
			if (log.getHandleMsg()!=null && log.getHandleMsg().length()>1500) {
				log.setHandleMsg(log.getHandleMsg().substring(0, 1500));
			}
			
			ferraliJobLogDao.updateHandleInfo(log);
			Logger.info("JobLogController.triggerLog success, triggerLogId:{}, status:{}, msg:{}", triggerLogId, status, msg);
			return ReturnT.SUCCESS;
		}
		return ReturnT.FAIL;
	}
	
	// 点评ferrali定制接口
	@RequestMapping("/ferrarifeedback")
	@ResponseBody
	public String ferrarifeedback(String result) {
		Logger.info("JobLogController.ferrarifeedback, result:{}", result);
		if (StringUtils.isNotBlank(result)) {
			FerrariFeedback resultBean = JacksonUtil.readValue(result, FerrariFeedback.class);
			if (resultBean!=null) {
				ReturnT<String> ret = this.triggerLog(Integer.valueOf(resultBean.getUuid()), resultBean.isStatus()?HttpUtil.SUCCESS:HttpUtil.FAIL, resultBean.getErrormsg());
				if (ret!=null && ret.getCode() == ReturnT.SUCCESS.getCode()) {
					return "ok";
				}
			}
		}
		return "fail";
	}
	
	@RequestMapping
	public String index(Model model, String jobName, String filterTime) {
		model.addAttribute("jobName", jobName);
		model.addAttribute("filterTime", filterTime);
		return "joblog/index";
	}
	
	@RequestMapping("/pageList")
	@ResponseBody
	public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,  
			@RequestParam(required = false, defaultValue = "10") int length,
			String jobName, String filterTime) {
		// parse param
		Date triggerTimeStart = null;
		Date triggerTimeEnd = null;
		if (StringUtils.isNotBlank(filterTime)) {
			String[] temp = filterTime.split(" - ");
			if (temp!=null && temp.length == 2) {
				try {
					triggerTimeEnd = DateUtils.parseDate(temp[0], new String[]{"yyyy-MM-dd HH:mm:ss"});
					triggerTimeEnd = DateUtils.parseDate(temp[1], new String[]{"yyyy-MM-dd HH:mm:ss"});
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		
		// page query
		List<FerrariJobLog> list = ferraliJobLogDao.pageList(start, length, jobName, triggerTimeStart, triggerTimeEnd);
		int list_count = ferraliJobLogDao.pageListCount(start, length, jobName, triggerTimeStart, triggerTimeEnd);
		
		// package result
		Map<String, Object> maps = new HashMap<String, Object>();
	    maps.put("recordsTotal", list_count);	// 总记录数
	    maps.put("recordsFiltered", list_count);// 过滤后的总记录数
	    maps.put("data", list);  				// 分页列表
		return maps;
	}
	
}
