/*******************************************************************************
 * Copyright (C) 2006-2013 AITIA International, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ai.aitia.meme.paramsweep.platform.simphony.impl;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import repast.simphony.parameter.ParameterFormatException;
import repast.simphony.parameter.ParameterSetter;
import repast.simphony.parameter.ParameterTreeSweeper;
import repast.simphony.parameter.Parameters;
import repast.simphony.parameter.ParametersCreator;
import repast.simphony.parameter.xml.ConstantBooleanSetterCreator;
import repast.simphony.parameter.xml.ConstantNumberSetterCreator;
import repast.simphony.parameter.xml.ConstantStringSetterCreator;
import repast.simphony.parameter.xml.ListSetterCreator;
import repast.simphony.parameter.xml.NumberSetterCreator;
import repast.simphony.parameter.xml.ParameterSetterCreator;
import repast.simphony.parameter.xml.ParameterSweepParser;
import repast.simphony.parameter.xml.SetterConstants;
import repast.simphony.util.collections.Pair;

public class AitiaParameterSweepParser extends ParameterSweepParser {

	//====================================================================================================
	// members

	private static final String PARAMETER_NAME = "parameter";
	private static final String SWEEP = "sweep";
	private String paramsXmlString;
	private Stack<ParameterSetter> stack = new Stack<ParameterSetter>();
	private ParameterTreeSweeper sweeper = new ParameterTreeSweeper();
	private Map<String, ParameterSetterCreator> creators = new HashMap<String, ParameterSetterCreator>();
	private Map<String, ParameterSetterCreator> constantCreators = new HashMap<String, ParameterSetterCreator>();
	private ParametersCreator creator = new ParametersCreator();
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public AitiaParameterSweepParser(String paramsXmlString) {
		super(null);
		this.paramsXmlString = paramsXmlString;
		creators.put("number", new NumberSetterCreator());
		creators.put("list", new ListSetterCreator());
		constantCreators.put("number", new ConstantNumberSetterCreator());
		constantCreators.put("string", new ConstantStringSetterCreator());
		constantCreators.put("String", new ConstantStringSetterCreator());
		constantCreators.put("boolean", new ConstantBooleanSetterCreator());
	}

	//----------------------------------------------------------------------------------------------------
	public AitiaParameterSweepParser(ParameterTreeSweeper sweeper, String paramsXmlString) {
		this(paramsXmlString);
		this.sweeper = sweeper;
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public Pair<Parameters, ParameterTreeSweeper> parse() throws ParserConfigurationException, SAXException, IOException {
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		parser.parse(new InputSource(new StringReader(paramsXmlString)),this);
		return new Pair<Parameters,ParameterTreeSweeper>(creator.createParameters(),sweeper);
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public void registerStepperCreator(String typeID, ParameterSetterCreator creator) {
		creators.put(typeID,creator);
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public void registerConstantCreator(String constantTypeID, ParameterSetterCreator creator) {
		constantCreators.put(constantTypeID, creator);
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equals(SWEEP)) {
			sweeper.setRunCount(Integer.parseInt(attributes.getValue("runs")));
		} else if (qName.equals(PARAMETER_NAME)) {
			try {
				String type = attributes.getValue("type");
				ParameterSetterCreator setterCreator = null;
				if (type.equals(SetterConstants.CONSTANT_ID)) {
					String cType = attributes.getValue(SetterConstants.CONSTANT_TYPE_NAME);
					setterCreator = constantCreators.get(cType);
				} else {
					setterCreator = creators.get(type);
				}

				if (setterCreator == null) {
					throw new SAXException(new ParameterFormatException("Invalid parameter '"
									+ attributesToString(attributes) + "'"));
				}

				setterCreator.init(attributes);
				setterCreator.addParameter(this.creator);
				ParameterSetter setter = setterCreator.createSetter();

				if (stack.isEmpty()) {
					ParameterSetter root = sweeper.getRootParameterSetter();
					sweeper.add(root, setter);
				} else {
					sweeper.add(stack.peek(), setter);
				}
				stack.push(setter);
			} catch (ParameterFormatException ex) {
				SAXException e = new SAXException(ex);
				e.initCause(ex);
				throw e;
			}
		}
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equals(PARAMETER_NAME)) stack.pop();
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private String attributesToString(Attributes attributes) {
		StringBuilder builder = new StringBuilder("[");
		for (int i = 0; i < attributes.getLength(); i++) {
			if (i != 0) builder.append(", ");
			builder.append(attributes.getQName(i));
			builder.append("=\"");
			builder.append(attributes.getValue(i));
			builder.append("\"");
		}
		builder.append("]");
		return builder.toString();
	}
}
