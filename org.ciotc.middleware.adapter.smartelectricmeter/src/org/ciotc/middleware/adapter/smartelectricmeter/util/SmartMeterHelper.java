/**
 *
 * SmartMeterHelper.java
 * ZhangMin.name - zhangmin@zhangmin.name
 * org.ciotc.middleware.adapter.smartelectricmeter
 *
 */
package org.ciotc.middleware.adapter.smartelectricmeter.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.ciotc.middleware.adapter.smartelectricmeter.bean.SmartMeter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author ZhangMin.name
 *
 */
public class SmartMeterHelper extends DefaultHandler{

	private List<SmartMeter> meters;
    private SmartMeter meter;
    private boolean usage = false;
    private int usageOrder = 0;
    public SmartMeterHelper(){
    	 meters = new ArrayList<SmartMeter>();
    }
    
	@Override
	public void startDocument() throws SAXException {
		System.out.println("start document");
		
	}

	public List<SmartMeter> getMeters(InputStream is){
		SAXParserFactory factory = null;
		SAXParser parser = null;
		SmartMeterHelper smh = new SmartMeterHelper();
		try {
			factory = SAXParserFactory.newInstance();
			parser = factory.newSAXParser();
			parser.parse(is,smh);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return smh.getMeters();
	}
	public List<SmartMeter> getMeters(){
		return meters;
	}
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException { 
		System.out.println(qName + "," + attributes.getValue(0) + "," + attributes.getValue(1));
		 if(qName.equalsIgnoreCase("meterrealtimedata")){
			 meter = new SmartMeter();
			 meter.setMeterID(attributes.getValue(0));
			 meter.setMeterName(attributes.getValue(1));
		 }
		 if(qName.equalsIgnoreCase("usage")){
				usage = true;
		 }else{
				usage = false;
		 }
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		System.out.println("End element:" + qName);
		if(qName.equalsIgnoreCase("meterrealtimedata")){
			meters.add(meter);
			meter = null;
			usageOrder = 0;
		}
		 if(qName.equalsIgnoreCase("usage")){
				usage = false;
				
		 }
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		System.out.println("in char:" + new String(ch,start,length));
			//float value = Float.parseFloat(new String(ch,start,length));
		if(usage){
			System.out.println("Usage in char:" + new String(ch,start,length));
			usageOrder ++;
			String value = new String(ch,start,length);
			if(usageOrder == 1){
				meter.setValue1(value);
			}else if(usageOrder == 2){
				meter.setValue2(value);
			}else if(usageOrder == 3){
				meter.setValue3(value);
			}else {
				meter.setValue4(value);
			}
			//meter.setValue1(Float.parseFloat(new String(ch,start,length)));
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SmartMeterHelper smh = new SmartMeterHelper();
		List<SmartMeter> meters = new ArrayList<SmartMeter>();
		InputStream is = smh.getClass().getClassLoader().getResourceAsStream("data.xml");
		meters = smh.getMeters(is);
		System.out.println("===Print Result===");
		for(SmartMeter sm : meters){
			System.out.println(sm.getMeterID() + "," + sm.getMeterName() + "," + sm.getValue1());
		}
	}

}
