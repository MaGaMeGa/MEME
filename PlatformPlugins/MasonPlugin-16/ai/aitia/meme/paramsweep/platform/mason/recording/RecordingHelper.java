/**
 * 
 */
package ai.aitia.meme.paramsweep.platform.mason.recording;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.reflections.ReflectionUtils;

import sim.engine.SimState;
import sim.engine.Steppable;
import ai.aitia.meme.Logger;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.generator.IStatisticInfoGenerator;
import ai.aitia.meme.paramsweep.generator.InnerOperatorsInfoGenerator;
import ai.aitia.meme.paramsweep.generator.OperatorsInfoGenerator;
import ai.aitia.meme.paramsweep.generator.StatisticsInfoGenerator;
import ai.aitia.meme.paramsweep.gui.info.InnerOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MultiColumnOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.SimpleGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.operatorPlugin.Operator_MultiColumnRecordable;
import ai.aitia.meme.paramsweep.platform.mason.impl.IMasonGeneratedModel;
import ai.aitia.meme.paramsweep.platform.mason.impl.MasonModelGenerator;
import ai.aitia.meme.paramsweep.platform.mason.recording.annotation.Recorder;
import ai.aitia.meme.paramsweep.platform.mason.recording.annotation.Recorder.OutputTime;
import ai.aitia.meme.paramsweep.platform.mason.recording.annotation.Recorder.RecordTime;
import ai.aitia.meme.paramsweep.platform.mason.recording.annotation.RecorderSource;
import ai.aitia.meme.paramsweep.platform.mason.recording.annotation.Submodel;
import ai.aitia.meme.paramsweep.platform.repast.impl.IGeneratedModel;
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;
import ai.aitia.meme.paramsweep.settingsxml.Block;
import ai.aitia.meme.paramsweep.settingsxml.BlockElement;
import ai.aitia.meme.paramsweep.settingsxml.BlockElementComplex;
import ai.aitia.meme.paramsweep.settingsxml.BlockElementText;
import ai.aitia.meme.paramsweep.settingsxml.BlockElementTypes;
import ai.aitia.meme.paramsweep.settingsxml.BuildBlocks;
import ai.aitia.meme.paramsweep.settingsxml.Member;
import ai.aitia.meme.paramsweep.settingsxml.ObjectFactory;
import ai.aitia.meme.paramsweep.settingsxml.Parent;
import ai.aitia.meme.paramsweep.settingsxml.Recorders;
import ai.aitia.meme.paramsweep.settingsxml.RecordersPage;
import ai.aitia.meme.paramsweep.settingsxml.Script;
import ai.aitia.meme.paramsweep.settingsxml.ScriptTypes;
import ai.aitia.meme.paramsweep.settingsxml.Scripts;
import ai.aitia.meme.paramsweep.settingsxml.StopData;
import ai.aitia.meme.paramsweep.settingsxml.Time;
import ai.aitia.meme.paramsweep.settingsxml.WritingTime;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;

/**
 * @author Tamás Máhr
 *
 */
public class RecordingHelper {

	private static class RecordingInfo {
		protected String memberName;
		
		protected Class<?> memberType;
		
		protected String recordingName;

		/**
		 * @param memberName
		 * @param memberType
		 * @param recordingName
		 */
		public RecordingInfo(String memberName, Class<?> memberType, String recordingName) {
			super();
			this.memberName = memberName;
			this.memberType = memberType;
			this.recordingName = recordingName;
		}

		/**
		 * @return the memberName
		 */
		public String getMemberName() {
			return memberName;
		}

		/**
		 * @return the memberType
		 */
		public Class<?> getMemberType() {
			return memberType;
		}

		/**
		 * @return the recordingName
		 */
		public String getRecordingName() {
			return recordingName;
		}

		/** {@inheritDoc} 
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((memberName == null) ? 0 : memberName.hashCode());
			return result;
		}

		/** {@inheritDoc} 
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RecordingInfo other = (RecordingInfo) obj;
			if (memberName == null) {
				if (other.memberName != null)
					return false;
			} else if (!memberName.equals(other.memberName))
				return false;
			return true;
		}

		/** {@inheritDoc} 
		 */
		@Override
		public String toString() {
			return "RecordingInfo [memberName=" + memberName + ", memberType=" + memberType + ", recordingName=" + recordingName + "]";
		}
		
		
	}

//	private static RecordingHelper instance = null;
	
	protected static final Map<String, Class<?>> statisticsPluginNames = new HashMap<String, Class<?>>();
	
	private static int serial = 0;
	
	protected static final Map<String, IStatisticInfoGenerator> infoGenerators = new HashMap<String, IStatisticInfoGenerator>();
	
	protected static final Map<String, String> statisticsNames = new HashMap<String, String>();

	private static OperatorsInfoGenerator operatorsInfoGenerator = new OperatorsInfoGenerator(new Operator_MultiColumnRecordable());
	
	private static InnerOperatorsInfoGenerator innerOperatorInfoGenerator = new InnerOperatorsInfoGenerator();
	
	protected List<RecorderInfo> recorders = new ArrayList<RecorderInfo>();

	protected RecordersPage recordersPage = null;

	private ObjectFactory objFactory = new ObjectFactory();

	private Map<Class<?>, Set<RecordingInfo>> annotatedRecordables = new HashMap<Class<?>, Set<RecordingInfo>>();
	
	private Map<String, List<String>> usedRecorderSources = new HashMap<String, List<String>>();
	
	private boolean recorderClosed = false;
	
	protected String memeGeneratedModelName = null;

	protected Class<?> generatedModelClass;

	protected IMasonGeneratedModel generatedModel;

	private boolean writeSource = false;
	
	protected Thread shutdownThread;
	
	static {
		try {
			statisticsPluginNames.put("mean", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_Mean"));
			statisticsPluginNames.put("avg", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_Mean"));
			statisticsPluginNames.put("min", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_Min"));
			statisticsPluginNames.put("max", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_Max"));
			statisticsPluginNames.put("sum", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_Sum"));
			statisticsPluginNames.put("kurtosis", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_Kurtosis"));
			statisticsPluginNames.put("median", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_Median"));
			statisticsPluginNames.put("product", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_Product"));
			statisticsPluginNames.put("variance", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_Variance"));
			statisticsPluginNames.put("sd", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_StandardDeviation"));
			statisticsPluginNames.put("skew", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_Skew"));
			statisticsPluginNames.put("se", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_StandardError"));
			statisticsPluginNames.put("stde", Class.forName("ai.aitia.meme.paramsweep.coltPlugin.Colt_StandardError"));
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	public RecordingHelper(final SimState model){
		this(model, false);
	}
	
	public RecordingHelper(final SimState model, final boolean writeSource){
		this.writeSource = writeSource;
		
		buildRecorderInfo(model);
	}

//	public static RecordingHelper newInstance(final SimState model){
//		return newInstance(model, false);
//	}
//	
//	public static RecordingHelper newInstance(final SimState model, final boolean writeSource){
//		// if the model was instantiated by MEME, just update the submodels 
//		if (model.getClass().getName().contains(MasonModelGenerator.modelSuffix) && IGeneratedModel.class.isAssignableFrom(model.getClass())){
//			return instance;
//		}
//
//		instance = new RecordingHelper(model, writeSource);
// 
////		if (instance.recorders.size() > 0){
////			instance.createGeneratedModel(model);
////		}
//		
//		return instance;
//	}
	
//	/**
//	 * Returns the last created {@link RecordingHelper} instance.
//	 * 
//	 * @return the last created {@link RecordingHelper} instance
//	 */
//	public static RecordingHelper getInstance(){
//		return instance;
//	}

	public List<RecorderInfo> getRecorders(){
		return recorders;
	}
	public static List<RecorderInfo> getRecorders(final SimState model){
		return getRecorders(model, false);
	}
	
	public static List<RecorderInfo> getRecorders(final SimState model, final boolean writeSource){
//		if (instance == null){
			RecordingHelper instance = new RecordingHelper(model, writeSource);
//		}
		
		return instance.recorders;
	}

	public static String getRecorderPageXML(final SimState model){
		return getRecorderPageXML(model, false);
	}
	
	public static String getRecorderPageXML(final SimState model, final boolean writeSource){
//		if (instance == null){
			RecordingHelper instance = new RecordingHelper(model, writeSource);
//		}
		
		if (instance.recordersPage == null || instance.recordersPage.getRecorders() == null){
			return null;
		}
		
		try {
			JAXBContext jContext = JAXBContext.newInstance(RecordersPage.class.getPackage().getName(), RecordersPage.class.getClassLoader());
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			jContext.createMarshaller().marshal(instance.recordersPage, buffer);
			String xmlString = new String(buffer.toByteArray(), "UTF-8");
			return xmlString;
		} catch (JAXBException e) {
			throw new IllegalStateException(e);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private void buildRecorderInfo(final SimState recorderObj){
		RecorderInfo recorderInfo = new RecorderInfo();
		recordersPage = new RecordersPage();
		ai.aitia.meme.paramsweep.settingsxml.Recorder recorder = new ai.aitia.meme.paramsweep.settingsxml.Recorder();
		Recorder recorderAnnotation = recorderObj.getClass().getAnnotation(Recorder.class);
		Map<String, List<String>> requiredSources = new HashMap<String, List<String>>();
		
		// MEME seems to remove the annotation during manipulating a descendant class
		if (recorderAnnotation == null){
			return;
		}
		
		for (String requiredSource : recorderAnnotation.sources()) {
			int startPar = requiredSource.indexOf('(');
			String stat = null;
			String source = requiredSource;
			
			if (startPar > 0){
				int endPar = requiredSource.lastIndexOf(')');
				source = requiredSource.substring(startPar + 1, endPar);
				stat = requiredSource.substring(0, startPar);
			}

			List<String> stats = requiredSources.get(source);
			if (stats == null){
				stats = new ArrayList<String>();
				requiredSources.put(source, stats);
			}
			stats.add(stat);
		}
		
		recorderInfo.setDelimiter("|");
		File recorderFile = new File(recorderAnnotation.value());
		String recorderFileName;
		if (recorderFile.getParent() == null){
			recorderFileName = System.getProperty("java.io.tmpdir") + File.separator + recorderFile.getName();
//			System.out.println("Recorder file: " + recorderFileName);
		} else {
			recorderFileName = recorderFile.getPath();
		}
		recorderInfo.setName(recorderFileName);
		recorderInfo.setOutputFile(new File(recorderFileName));
		String recordType = recorderAnnotation.recordAt().getMemeValue();
		if (recorderAnnotation.recordAt() == RecordTime.EVERY_NTH_ITERATION){
			recordType += ": " + recorderAnnotation.recordingInterval();
		} else if (recorderAnnotation.recordAt() == RecordTime.CONDITIONAL){
			recordType += ": " + recorderAnnotation.condition();
		}
		recorderInfo.setRecordType(recordType);
		String writeType = recorderAnnotation.outputAt().getMemeValue();
		if (recorderAnnotation.outputAt() == OutputTime.EVERY_NTH_ITERATION){
			writeType += ": " + recorderAnnotation.outputInterval();
		}
		recorderInfo.setWriteType(writeType);
		
		List<RecordableInfo> recordables = new ArrayList<RecordableInfo>();
		recorderInfo.setRecordables(recordables);
		recorders.add(recorderInfo);
		
		StopData stopData = new StopData();
		stopData.setCondition(false);
		recordersPage.setStopData(stopData);
		recordersPage.setUserVariables("");
		recorder.setName(recorderFileName);
		recorder.setOutput(recorderFileName);
		
		Time time = new Time();
		time.setType(recordType);
		recorder.setTime(time);
		
		
		WritingTime writingTimeType = new WritingTime();
		writingTimeType.setType(writeType);
		recorder.setWritingTime(writingTimeType );
		
		
		Recorders recordersType = new Recorders();
		recordersType.setRecorder(recorder);
		recordersPage.setRecorders(recordersType);
		
		Scripts scripts = new Scripts();
		recordersPage.setScripts(scripts );
		
		findSources(recorderObj, "", requiredSources, recorder, recorderInfo, recordables);
		
		
		sortRecorderMembers(recorderInfo, recorder, recorderAnnotation.sources());
		
		// check whether all sources are found
		for (String requiredSource : requiredSources.keySet()){
			if (! usedRecorderSources.containsKey(requiredSource)){
				Logger.logWarning(requiredSource + " is listed in @Recorder(sources=), but no corresponding @RecorderSource annotation has been found!");
			}
		}
		
	}
	
	private void findSources(final Object objectToExplore,final String objectAccessor, final Map<String, List<String>> requiredSources,
			final ai.aitia.meme.paramsweep.settingsxml.Recorder recorder, final RecorderInfo recorderInfo, final List<RecordableInfo> recordables) {
		
		Class<? extends Object> classToExplore = objectToExplore.getClass();
		
		// find RecorderSource values
		@SuppressWarnings("unchecked")
		Set<Method> sourceMethods = ReflectionUtils.getAllMethods(classToExplore, ReflectionUtils.withAnnotation(RecorderSource.class));
		
		for (Method method : sourceMethods) {
			RecorderSource annotation = method.getAnnotation(RecorderSource.class);
//			if (!requiredSources.containsKey(annotation.value())){
//				continue;
//			}
			
			if (Modifier.isPrivate(method.getModifiers())){
				throw new IllegalArgumentException("Method '" + method.getReturnType() + " " + method.getName() + "("
						+ Arrays.toString(method.getParameterTypes()) + ")' is private, only public or protected methods can be recorded.");
			}
			
			// if this method is not in the model (but in a submodel), then it should be public!
			if (!SimState.class.isAssignableFrom(classToExplore)){
				if (!Modifier.isPublic(method.getModifiers())){
					throw new IllegalArgumentException("Method '" + method.getReturnType() + " " + method.getName() + "("
							+ Arrays.toString(method.getParameterTypes()) + ")' is not public, only public methods can be recorded in sub-models.");
				}
			}

			
			if (method.getParameterTypes().length > 0){
				throw new IllegalArgumentException("Method '" + method.getReturnType() + " " + method.getName() + "("
						+ Arrays.toString(method.getParameterTypes()) + ")' cannot be recorded, only methods with no parameters can be recorded.");
			}
			
			if (method.getReturnType().equals(Void.TYPE)
					|| (!Util.isAcceptableType(method.getReturnType()))) {
				throw new IllegalArgumentException(
						"Method '"
								+ method.getReturnType()
								+ " "
								+ method.getName()
								+ "("
								+ Arrays.toString(method.getParameterTypes())
								+ ")' cannot be recorded, only methods with an acceptable return type (primitive types, String, or array or Collectionof such types) can be recorded.");
			}

			Class<?> innerType = annotation.innerType();
			if (innerType.equals(Object.class)){
				if (Collection.class.isAssignableFrom(method.getReturnType())){
					Type genericReturnType = method.getGenericReturnType();
					if (genericReturnType instanceof ParameterizedType) {
						ParameterizedType parameterizedReturnType = (ParameterizedType) genericReturnType;
						innerType = (Class<?>) parameterizedReturnType.getActualTypeArguments()[0];
					}
				}
				
				if (method.getReturnType().isArray()){
					innerType = method.getReturnType().getComponentType();
				}
			}

			Set<RecordingInfo> recordableMembers = null;
			
			if (!innerType.equals(Object.class) && !Util.isAcceptableSimpleType(innerType)){
				recordableMembers = collectRecordables(innerType);
			} else {
				recordableMembers = new HashSet<RecordingInfo>();
			}
			
			if (!annotation.value().isEmpty() && (Util.isAcceptableSimpleType(innerType) || !annotation.member().isEmpty())){
				recordableMembers.add(new RecordingInfo(annotation.member(), null, ""));
			}

			if (!annotation.value().isEmpty() && Util.isAcceptableSimpleType(method.getReturnType())){
				recordableMembers.add(new RecordingInfo(method.getName(), method.getReturnType(), ""));
			}

			for (RecordingInfo info : recordableMembers){
				String innerMemberName = info.getMemberName();
				Class<?> innerMemberType = info.getMemberType();
				try {
					if (innerMemberType == null){
						if (innerMemberName.endsWith("()")){
							Method innerMemberMethod;
							innerMemberMethod = innerType.getMethod(innerMemberName.substring(0, innerMemberName.length() - 2));
							if (innerMemberMethod != null){
								innerMemberType = innerMemberMethod.getReturnType();
							}
						} else if (!innerMemberName.isEmpty()){
							Field innerMemberField = innerType.getField(innerMemberName);
							if (innerMemberField != null){
								innerMemberType = innerMemberField.getType(); 
							}
						}
					}
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				}

				if (!requiredSources.containsKey(annotation.value() + info.getRecordingName())){
					continue;
				}
			
				for (String statName : requiredSources.get(annotation.value() + info.getRecordingName())){
					if (statName != null){
						if (!method.getReturnType().isArray() && !Collection.class.isAssignableFrom(method.getReturnType())){
							throw new IllegalArgumentException(
									"Method '"
											+ method.getReturnType()
											+ " "
											+ method.getName()
											+ "("
											+ Arrays.toString(method.getParameterTypes())
											+ ")' cannot be aggregated (" + statName + "), only methods with a Collection or array return type can be aggregated.");
						}

						//addStatistics(recorder, recorderInfo, annotation.value(), statName, method.getName() + "()", method.getReturnType(), innerType, innerMemberName, innerMemberType);
						addStatistics(recorder, recorderInfo, annotation.value() + info.getRecordingName(), statName, objectAccessor + method.getName() + "()",
								method.getReturnType(), innerType, innerMemberName, innerMemberType);

					} else {
						if (!method.getReturnType().isArray() && !Collection.class.isAssignableFrom(method.getReturnType())
								&& !Map.class.isAssignableFrom(method.getReturnType())) {
							String annotationValue = annotation.value() + info.getRecordingName();
							if (requiredSources.containsKey(annotationValue)){
								recordables.add(new RecordableInfo(annotation.value() + info.getRecordingName(), method.getReturnType(), null, objectAccessor + method.getName() + "()"));

								Member member = new Member();
								member.setAlias(annotation.value() + info.getRecordingName());
								member.setJavaType(method.getReturnType().getName());
								member.setType(Utilities.toTypeString2(method.getReturnType()));
								member.setContent(method.getName() + "()");
								recorder.getMember().add(member);

								addUsedRecorderSource(annotationValue, objectAccessor + method.getName());
								checkUsedRecorderSources(annotationValue);
}
						} else { // this is a multicolumn recordable
							int collectionLength = annotation.collectionLength();
							String collectionLengthMember = annotation.collectionLengthMember();
							
							if (collectionLength == 0 && collectionLengthMember.isEmpty()){
								throw new IllegalArgumentException("Either the collectionLength (!= 0) or the collectionLengthMember (!= \"\") attribute should be set when recording collections or arrays (" + annotation.value() + info.getRecordingName() + ")");
							}
							//
							//							collectionLength = getCollectionLength(recorderObj, collectionLengthMember);
							//						}

							//						if (collectionLength <= 0){
							//							throw new IllegalArgumentException("You are trying to record a collection ("
							//									+ annotation.value()
							//									+ info.getRecordingName()
							//									+ ") of length 0"
							//									+ (!annotation.collectionLengthMember().isEmpty() ? " (collectionLengthMember = "
							//											+ annotation.collectionLengthMember() + ")" : ""));
							//						}

							RecorderInfo infoIfNeeded = null;

							if (requiredSources.containsKey(annotation.value() + info.getRecordingName())){
								infoIfNeeded = recorderInfo;
							}
							//						addMultiColumnOperator(recorder, recorderInfo, method.getName() + "()", method.getReturnType(),
							//								annotation.value(), innerType, annotation.member(), innerMemberType, collectionLength, annotation.NAFiller());
							addMultiColumnOperator(recorder, infoIfNeeded, objectAccessor + method.getName() + "()", method.getReturnType(),
									annotation.value() + info.getRecordingName(), innerType, innerMemberName, innerMemberType, collectionLength, objectAccessor + collectionLengthMember, annotation.NAFiller());
						}
					}
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		Set<Field> sourceFields = ReflectionUtils.getAllFields(classToExplore, ReflectionUtils.withAnnotation(RecorderSource.class));
		
		for (Field field : sourceFields) {
			RecorderSource annotation = field.getAnnotation(RecorderSource.class);
//			if (!requiredSources.containsKey(annotation.value())){
//				continue;
//			}
			
			if (Modifier.isPrivate(field.getModifiers())){
				throw new IllegalArgumentException("Field '" + field.getType() + " " + field.getName()
						+ "' is private, only public or protected fields can be recorded.");
			}
			
			if (!Util.isAcceptableType(field.getType())) {
				throw new IllegalArgumentException(
						"Field '"
								+ field.getType()
								+ " "
								+ field.getName()
								+ "' cannot be recorded, only fields with an acceptable type (primitive types, String, or array or Collection of such types) can be recorded.");
			}

			Class<?> innerType = annotation.innerType();
			if (innerType.equals(Object.class)){
				if (Collection.class.isAssignableFrom(field.getType())){
					Type genericReturnType = field.getGenericType();
					if (genericReturnType instanceof ParameterizedType) {
						ParameterizedType parameterizedReturnType = (ParameterizedType) genericReturnType;
						innerType = (Class<?>) parameterizedReturnType.getActualTypeArguments()[0];
					}
				}
				
				if (field.getType().isArray()){
					innerType = field.getType().getComponentType();
				}
			}

			
			Set<RecordingInfo> recordableMembers = null;
			
			if (!innerType.equals(Object.class) && !Util.isAcceptableSimpleType(innerType)){
				recordableMembers = collectRecordables(innerType);
			} else {
				recordableMembers = new HashSet<RecordingInfo>();
			}
			
			if (!annotation.value().isEmpty() && (Util.isAcceptableSimpleType(innerType) || !annotation.member().isEmpty())){
				recordableMembers.add(new RecordingInfo(annotation.member(), null, ""));
			}
			
			if (!annotation.value().isEmpty() && Util.isAcceptableSimpleType(field.getType())){
				recordableMembers.add(new RecordingInfo(field.getName(), field.getType(), ""));
			}
			
			for (RecordingInfo info : recordableMembers){
				String innerMemberName = info.getMemberName();
				Class<?> innerMemberType = info.getMemberType();
				try {
					if (innerMemberType == null){
						if (innerMemberName.endsWith("()")){
							Method innerMemberMethod;
							innerMemberMethod = innerType.getMethod(innerMemberName.substring(0, innerMemberName.length() - 2));
							if (innerMemberMethod != null){
								innerMemberType = innerMemberMethod.getReturnType();
							}
						} else if (! innerMemberName.isEmpty()){
							Field innerMemberField = innerType.getField(innerMemberName);
							if (innerMemberField != null){
								innerMemberType = innerMemberField.getType(); 
							}
						}
					}
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				}
				
				if (!requiredSources.containsKey(annotation.value() + info.getRecordingName())){
					continue;
				}

				String fieldName = field.getName();
				// if this field is not in the model (but in a submodel), then we should use a getter to reach it!
				if (!SimState.class.isAssignableFrom(classToExplore)){
					try {
						fieldName = classToExplore.getMethod("get" + Util.capitalize(field.getName())).getName() + "()";
					} catch (NoSuchMethodException e) {
						throw new IllegalArgumentException("Field '" + field.getName() 
								+ "' should have a getter to be used as a recorder source in " + classToExplore.getName() + ", which is used as a sub-model.");
					} catch (SecurityException e) {
						throw new IllegalStateException(e);
					}
				}

				for (String statName : requiredSources.get(annotation.value() + info.getRecordingName())){
					if (statName != null){
						if (!field.getType().isArray() && !Collection.class.isAssignableFrom(field.getType())){
							throw new IllegalArgumentException(
									"Field '"
											+ field.getType()
											+ " "
											+ field.getName()
											+ "' cannot be aggregated (" + statName + "), only fields of a Collection or array type can be aggregated.");
						}

						addStatistics(recorder, recorderInfo, annotation.value() + info.getRecordingName(), statName, objectAccessor + fieldName, field.getType(), innerType, innerMemberName, innerMemberType);
					} else {
						if (!field.getType().isArray() && !Collection.class.isAssignableFrom(field.getType()) && !Map.class.isAssignableFrom(field.getType())){
							String annotationValue = annotation.value() + info.getRecordingName();
							if (requiredSources.containsKey(annotationValue)){
								recordables.add(new RecordableInfo(annotationValue, field.getType(), null, objectAccessor + fieldName));

								Member member = new Member();
								member.setJavaType(field.getType().getName());
								member.setType(Utilities.toTypeString2(field.getType()));
								member.setContent(field.getName());
								member.setAlias(annotation.value() + info.getRecordingName());
								recorder.getMember().add(member);
								
								addUsedRecorderSource(annotationValue, objectAccessor + fieldName);
								checkUsedRecorderSources(annotationValue);
							}
						} else { // this is a multi-column field
							int collectionLength = annotation.collectionLength();
							String collectionLengthMember = annotation.collectionLengthMember();
							
							if (collectionLength == 0 && collectionLengthMember.isEmpty()){
								throw new IllegalArgumentException("Either the collectionLength (!= 0) or the collectionLengthMember (!= \"\") attribute should be set when recording collections or arrays (" + annotation.value() + info.getRecordingName() + ")");
							}
							
							//							collectionLength = getCollectionLength(recorderObj, collectionLengthMember);
							//						}

							//						if (collectionLength <= 0){
							//							throw new IllegalArgumentException("You are trying to record a collection ("
							//									+ annotation.value()
							//									+ info.getRecordingName()
							//									+ ") of length 0"
							//									+ (!annotation.collectionLengthMember().isEmpty() ? " (collectionLengthMember = "
							//											+ annotation.collectionLengthMember() + ")" : ""));
							//						}

							RecorderInfo infoIfNeeded = null;

							if (requiredSources.containsKey(annotation.value() + info.getRecordingName())){
								infoIfNeeded = recorderInfo;
							}

							addMultiColumnOperator(recorder, infoIfNeeded, objectAccessor + fieldName, field.getType(),
									annotation.value() + info.getRecordingName(), innerType, innerMemberName, innerMemberType, collectionLength, objectAccessor + collectionLengthMember,
									annotation.NAFiller());
						}
					}
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		Set<Field> subModelFields = ReflectionUtils.getAllFields(classToExplore, ReflectionUtils.withAnnotation(Submodel.class));
		for (Field field : subModelFields) {
			try {
				field.setAccessible(true);
				Method getter = classToExplore.getMethod("get" + Util.capitalize(field.getName()));
				Object objectToExplore2 = field.get(objectToExplore);
				if (objectToExplore2 != null){
					findSources(objectToExplore2, "((" + objectToExplore2.getClass().getName() + ")" + objectAccessor + getter.getName() + "()).", requiredSources, recorder, recorderInfo, recordables);
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException(e);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException("Submodel '" + field.getName() + "' has no getter/setter!"
						+ " Members annotated as @Submodel need to have getters/setters in order to be handled as parameters and to be able to record values from them.");
			} catch (SecurityException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private void sortRecorderMembers(final RecorderInfo recorderInfo, final ai.aitia.meme.paramsweep.settingsxml.Recorder recorderXml, final String[] sources){
		List<RecordableInfo> recordables = recorderInfo.getRecordables();
		List<RecordableInfo> newRecordables = new ArrayList<RecordableInfo>();
		List<Member> members = recorderXml.getMember();
		List<Member> newMembers = new ArrayList<Member>();
		
		for (String source : sources) {
			for (RecordableInfo recordableInfo : recordables) {
				String name = recordableInfo.getName();
				if (name.equals(source)){
					newRecordables.add(recordableInfo);
					
					//break; // don't break; it's possible to have multiple recorders with the same name
				}
				
				if (name.equals(source + Util.GENERATED_MODEL_MULTICOLUMN_POSTFIX)){
					newRecordables.add(recordableInfo);
						
					//break; // don't break; it's possible to have multiple recorders with the same name
				}
			}
			
			for (Member member : members) {
				String name = member.getAlias();
				if (name.equals(source)){
					newMembers.add(member);
					
					//break; // don't break; it's possible to have multiple recorders with the same name
				}
				
				if (name.equals(source + Util.GENERATED_MODEL_MULTICOLUMN_POSTFIX)){
					newMembers.add(member);
						
					//break; // don't break; it's possible to have multiple recorders with the same name
				}
			}
		}
		
		recorderInfo.setRecordables(newRecordables);
		members.clear();
		members.addAll(newMembers);
	}

	private void createGeneratedModel(final SimState model) {
		try {
			ClassPool pool = ClassPool.getDefault();
			CtClass ancestor = pool.get(model.getClass().getName());
			@SuppressWarnings("rawtypes")
			List<AbstractParameterInfo> newParameters = new ArrayList<AbstractParameterInfo>();
			String directory = new File(ancestor.getURL().toURI()).getCanonicalPath();
			directory = directory.replace(ancestor.getName().replaceAll("\\.", Matcher.quoteReplacement(File.separator)), "");
			directory = directory.substring(0, directory.lastIndexOf('.') - 1);
			MasonModelGenerator masonModelGenerator = new MasonModelGenerator(pool, ancestor, recorders, "false", false, newParameters, directory, null);
			memeGeneratedModelName = masonModelGenerator.getGeneratedModelName();
			try{
				generatedModelClass = Class.forName(memeGeneratedModelName);
			} catch (ClassNotFoundException e){
				String error = masonModelGenerator.generateModel(false);
				if (error != null){
					throw new IllegalStateException(error);
				}

				if (writeSource ){
					error = masonModelGenerator.writeSource();
				}
				if (error != null){
					throw new IllegalStateException(error);
				}
				generatedModelClass = Class.forName(memeGeneratedModelName);
			}
			
			Method setModelMethod = generatedModelClass.getMethod("setModel", model.getClass());

			generatedModel = (IMasonGeneratedModel) generatedModelClass.newInstance();
			setModelMethod.invoke(generatedModel, model);

			generatedModel.aitiaGenerated_setRun(1);
			//					return  generatedModel;

//			scheduleRecording(model);
			
			shutdownThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					closeRecorder();
				}
			});
			Runtime.getRuntime().addShutdownHook(shutdownThread);
			
		} catch (NotFoundException e) {
			throw new IllegalStateException(e);
		} catch (InstantiationException e) {
			throw new IllegalStateException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		} catch (SecurityException e) {
			throw new IllegalStateException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalStateException(e);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public void scheduleRecording(final SimState model) {
		// if the model was instantiated by MEME, do nothing 
		if (model.getClass().getName().contains(MasonModelGenerator.modelSuffix) && IGeneratedModel.class.isAssignableFrom(model.getClass())){
			return;
		}
		
		for (RecorderInfo recorder : recorders) {
			System.out.println(recorder.getOutputFile().getAbsolutePath());
		}
		
		createGeneratedModel(model);

		model.schedule.scheduleRepeating(new Steppable() {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void step(SimState state) {
				generatedModel.stepEnded();
			}
		}, Integer.MAX_VALUE, 1);
	}

	public void closeRecorder(){
		if (!recorderClosed){
			recorderClosed = true;
			if (generatedModel != null){
				
				generatedModel.simulationStop();
				generatedModel.aitiaGenerated_writeEnd();
				generatedModel = null;
			}
			
			// when called from tomcat (by PET), we need to unregister the shutdown hook to release all resources
			if (shutdownThread != null){
				Runtime.getRuntime().removeShutdownHook(shutdownThread);
				shutdownThread = null;
			}
		}
	}
	
	/************************************   private methods   ****************************/
	
	private Set<RecordingInfo> collectRecordables(Class<?> type) {
		
		Set<RecordingInfo> result = annotatedRecordables.get(type);
		
		if (result != null){
			return result;
		}
		
		result = new HashSet<RecordingInfo>();
		annotatedRecordables.put(type, result);
		
		@SuppressWarnings("unchecked")
		Set<Method> methods = ReflectionUtils.getAllMethods(type, ReflectionUtils.withAnnotation(RecorderSource.class));
		for (Method method : methods) {
			result.add(new RecordingInfo(method.getName() + "()", method.getReturnType(), method.getAnnotation(RecorderSource.class).value()));
		}
		
		@SuppressWarnings("unchecked")
		Set<Field> fields = ReflectionUtils.getAllFields(type, ReflectionUtils.withAnnotation(RecorderSource.class));
		for (Field field : fields) {
			result.add(new RecordingInfo(field.getName(), field.getType(), field.getAnnotation(RecorderSource.class).value()));
		}
		
		return result;
	}

	private void addMultiColumnOperator(ai.aitia.meme.paramsweep.settingsxml.Recorder recorder, RecorderInfo recorderInfo, final String memberName,
			final Class<?> memberType, final String annotationValue, final Class<?> innerType, final String innerMemberName, final Class<?> innerMemberType, final int collectionLength, 
			String collectionLengthMember, final String NAFiller) {
		
		if (!memberType.isArray() && !Collection.class.isAssignableFrom(memberType)){
			throw new IllegalArgumentException(
					"Member '"
							+ memberType.getName()
							+ " "
							+ memberName
							+ "' cannot be recorded as a multi-column recordable, only members with array or Collection types can be recorded as multi-column recordable.");
		}
		
		if (!Util.isAcceptableSimpleType(innerType) && !Util.isAcceptableSimpleType(innerMemberType)){
			throw new IllegalArgumentException(
					"Member '"
							+ memberType.getName()
							+ " "
							+ memberName
							+ "' (inner type: '" + innerType + "', accessor: '" + (innerMemberType != null ? innerMemberType.getName() : "<unknown type>") + " " + 
							(innerMemberName != null ? innerMemberName : "<unknown member>")+ 
									"') cannot be recorded, only members with array or Collection of primitive types can be recorded as multi-column recordable.");
		}
		
		MemberInfo methodMemberInfo = new MemberInfo(memberName, memberType.getSimpleName(), memberType);
		methodMemberInfo.setInnerType(innerType);
		
		if (collectionLength > 0){
			collectionLengthMember = String.valueOf(collectionLength);
		}
		
		Object[] operatorParams = new Object[6];
		
		operatorParams[0] = methodMemberInfo; // collection
		operatorParams[1] = innerType; // inner type
		operatorParams[2] = (innerMemberName != null && !innerMemberName.isEmpty()? new MemberInfo(innerMemberName, innerMemberType.getSimpleName(), innerMemberType) : ""); // member
		operatorParams[3] = collectionLengthMember; // recording length
		operatorParams[4] = NAFiller; // na filler
		operatorParams[5] = annotationValue+ "Multi()"; // filled by operatorsInfoGenerator.generateInfoObject()
		
		MultiColumnOperatorGeneratedMemberInfo operatorInfo = (MultiColumnOperatorGeneratedMemberInfo) operatorsInfoGenerator.generateInfoObject((String)operatorParams[5], operatorParams); 
		operatorInfo.setGeneratorName("ai.aitia.meme.paramsweep.operatorPlugin.Operator_MultiColumnRecordable");
		
		// do not query the length of the collection now, only at the first recording
//		StringBuffer alias = new StringBuffer();
//		for (int i = 0 ; i < collectionLength ; i++){
//			alias.append("\"" + annotationValue + "Multi_" + i + "\"|");
//		}
//		alias.delete(alias.length() - 2, alias.length());
//		alias.deleteCharAt(0);

		RecordableElement recordableElement = new RecordableElement(operatorInfo, (String)operatorParams[5]);
		
		if (recorderInfo != null){
			RecordableInfo recordableInfo = InfoConverter.convertRecordableElement2RecordableInfo(recordableElement);
			String accessorName = methodMemberInfo.getName();
			if (innerMemberName != null && !innerMemberName.isEmpty()){
				accessorName += "{" + innerType.getName() + " -> " + innerMemberName + "}";
			}
			addUsedRecorderSource(annotationValue, accessorName);
			checkUsedRecorderSources(annotationValue);
			recorderInfo.getRecordables().add(recordableInfo);
		}
		
		Script script = new Script();
		script.setName(operatorInfo.getName());
		script.setScriptType(ScriptTypes.OPERATOR);
		script.setInner(false);
		script.setJavaType(operatorInfo.getJavaType().getCanonicalName());
		script.setType(operatorInfo.getType());
		script.setMulticolumnLength(collectionLengthMember);
		script.setSource(operatorInfo.getSource());
		script.setReferences("");
		script.setGeneratorName(operatorInfo.getGeneratorName());
		BuildBlocks buildBlocks = new BuildBlocks();
		List<Block> xmlBlocksList = buildBlocks.getBlock();
		Block xmlBlock = new Block();
		xmlBlocksList.add(xmlBlock);
		List<JAXBElement<? extends BlockElement>> xmlBlockElements = xmlBlock.getBlockElement();

		// first add the method member returning the collection/array
		BlockElementComplex xmlBlockElementComplex = new BlockElementComplex();
		xmlBlockElementComplex.setBlockElementType(BlockElementTypes.MEMBER);
		xmlBlockElementComplex.setType(methodMemberInfo.getType());
		Class<?> type = methodMemberInfo.getInnerType();
		if (type != null){
			xmlBlockElementComplex.setInnerType(type.getName());
		} else {
			xmlBlockElementComplex.setInnerType("null");
		}
		xmlBlockElementComplex.setJavaType(methodMemberInfo.getJavaType().getName());
		xmlBlockElementComplex.setName(methodMemberInfo.getName());
		xmlBlockElements.add(objFactory.createBlockElement(xmlBlockElementComplex));
		
		// add the inner type of the collection
		BlockElementText xmlBlockElementText = new BlockElementText();
		xmlBlockElementText.setBlockElementType(BlockElementTypes.CLASS);
		xmlBlockElementText.setContent(innerType.getName());
		xmlBlockElements.add(objFactory.createBlockElement(xmlBlockElementText));

		if (Util.isAcceptableSimpleType(innerType)){
			xmlBlockElementText = new BlockElementText();
			xmlBlockElementText.setBlockElementType(BlockElementTypes.STRING);
			xmlBlockElementText.setContent("Cannot select members of " + innerType.getName());
			xmlBlockElements.add(objFactory.createBlockElement(xmlBlockElementText));
		} else {
			xmlBlockElementComplex = new BlockElementComplex();
			xmlBlockElementComplex.setBlockElementType(BlockElementTypes.MEMBER);
			xmlBlockElementComplex.setName(innerMemberName);
			xmlBlockElementComplex.setType(Utilities.toTypeString(innerMemberType));
			xmlBlockElementComplex.setJavaType(Utilities.toTypeString(innerMemberType));
			xmlBlockElementComplex.setInnerType("void"); // TODO why void??
			xmlBlockElements.add(objFactory.createBlockElement(xmlBlockElementComplex));
		}
		
		xmlBlockElementText = new BlockElementText();
		xmlBlockElementText.setBlockElementType(BlockElementTypes.STRING);
		xmlBlockElementText.setContent("" + collectionLengthMember);
		xmlBlockElements.add(objFactory.createBlockElement(xmlBlockElementText));

		xmlBlockElementText = new BlockElementText();
		xmlBlockElementText.setBlockElementType(BlockElementTypes.STRING);
		xmlBlockElementText.setContent(NAFiller);
		xmlBlockElements.add(objFactory.createBlockElement(xmlBlockElementText));

		
		script.setBuildBlocks(buildBlocks);
		script.setDisplayName(operatorInfo.getDisplayName());
		script.setReferences("");
		recordersPage.getScripts().getScript().add(script);

		if (recorderInfo != null){
//			addUsedRecorderSource(annotationValue, memberName);
//			checkUsedRecorderSources(annotationValue);

			Member member = new Member();
			member.setAlias(recordableElement.getAlias());
			member.setJavaType(operatorInfo.getJavaType().getCanonicalName());
			member.setType(operatorInfo.getType());
			member.setContent(operatorInfo.getName());
			recorder.getMember().add(member);
		}
	}

//	private int getCollectionLength(final Object recorderObj, String collectionLengthMember) {
//		int collectionLength;
//		if (collectionLengthMember.endsWith("()")){
//			String methodName = collectionLengthMember.substring(0, collectionLengthMember.length() - 2);
//			@SuppressWarnings("unchecked")
//			Set<Method> methods = ReflectionUtils.getAllMethods(recorderObj.getClass(), Predicates.and(ReflectionUtils.withName(methodName), ReflectionUtils
//					.withParametersCount(0)));
//			
//			// we have to find the method definition that belongs to the top-most model class
//			Method collectionLengthMethod = null;
//			if (methods.size() >= 1){
//				for (Class<?> cl = recorderObj.getClass() ; !cl.equals(Object.class) ; cl = cl.getSuperclass()){
//					for (Method m : cl.getDeclaredMethods()){
//						if (m.getName().equals(methodName) && m.getParameterTypes().length == 0){
//							collectionLengthMethod = m;
//							break;
//						}
//					}
//					
//					if (collectionLengthMethod != null){
//						break;
//					}
//				}
//			}
//			
//			if (collectionLengthMethod != null){	
//				try {
//					collectionLengthMethod.setAccessible(true);
//					collectionLength = (Integer) collectionLengthMethod.invoke(recorderObj);
//				} catch (IllegalArgumentException e) {
//					throw new IllegalArgumentException("Could not retrieve an int value from the method '" + recorderObj.getClass().getSimpleName() + "." + collectionLengthMethod.getName() + "'!", e);
//				} catch (IllegalAccessException e) {
//					throw new IllegalArgumentException("Could not retrieve an int value from the method '" + recorderObj.getClass().getSimpleName() + "." + collectionLengthMethod.getName() + "'!", e);
//				} catch (InvocationTargetException e) {
//					throw new IllegalArgumentException("Could not retrieve an int value from the method '" + recorderObj.getClass().getSimpleName() + "." + collectionLengthMethod.getName() + "'!", e);
//				} catch (ClassCastException e){
//					throw new IllegalArgumentException("Could not retrieve an int value from the method '" + recorderObj.getClass().getSimpleName() + "." + collectionLengthMethod.getName() + "'!", e);
//				}
//			} else {
//				throw new IllegalArgumentException("Could not retrieve an int value from the method'" + recorderObj.getClass().getSimpleName() + "." + collectionLengthMember + "', no such method found!");
//			}
//		} else {
//			@SuppressWarnings("unchecked")
//			Set<Field> fields = ReflectionUtils.getAllFields(recorderObj.getClass(), ReflectionUtils.withName(collectionLengthMember));
//
//			if (fields.size() == 1){
//				Field field = fields.iterator().next();
//				try {
//					field.setAccessible(true);
//					collectionLength = field.getInt(recorderObj);
//				} catch (IllegalArgumentException e) {
//					throw new IllegalArgumentException("Could not retrieve an int value from the field '" + recorderObj.getClass().getSimpleName() + "." + field.getName() + "'!", e);
//				} catch (IllegalAccessException e) {
//					throw new IllegalArgumentException("Could not retrieve an int value from the field '" + recorderObj.getClass().getSimpleName() + "." + field.getName() + "'!", e);
//				}
//			} else {
//				throw new IllegalArgumentException("Could not retrieve an int value from the field '" + recorderObj.getClass().getSimpleName() + "." + collectionLengthMember + "', no such field found!");
//			}
//		}
//		
//		return collectionLength;
//	}

//	/**
//	 * Checks whether the a {@link RecordableInfo} with the same name has already been added to the given {@link RecorderInfo}.	 * 
//	 * @param recorderInfo
//	 *            the recorder to check
//	 * @param recordableInfo
//	 *            the recordable that should be unique
//	 * @return true if the given {@link RecorderInfo} does not contain a {@link RecordableInfo} with the same name as the given one, false if it does
//	 */
//	private RecordableInfo getRecordable(RecorderInfo recorderInfo, String recordableName) {
//		List<RecordableInfo> recordables = recorderInfo.getRecordables();
//		
//		for (RecordableInfo existingRecordableInfo : recordables) {
//			if (existingRecordableInfo.getName().equals(recordableName)){
//				return existingRecordableInfo;
//			}
//		}
//		
//		return null;
//	}

	private void addStatistics(ai.aitia.meme.paramsweep.settingsxml.Recorder recorder, RecorderInfo recorderInfo, String annotationName,
			String statName, String memberAccessor, Class<?> memberType, final Class<?> innerType, final String innerTypeMemberName,
			final Class<?> innerTypeMemberType) {
		if (!memberType.isArray() && !Collection.class.isAssignableFrom(memberType)){
			throw new IllegalArgumentException(
					"Member '"
							+ memberType.getName()
							+ " "
							+ memberAccessor
							+ "' cannot be aggregated, only members with array or Collection types can be aggregated.");
		}
		
		if (!Util.isAcceptableSimpleType(innerType) && !Util.isAcceptableSimpleType(innerTypeMemberType)){
			throw new IllegalArgumentException(
					"Member '"
							+ memberType.getName()
							+ " "
							+ memberAccessor
							+ "' (inner type: '" + innerType + "', accessor: '" + (innerTypeMemberType != null ? innerTypeMemberType.getName() : "<unknown type>") + " " + 
							(innerTypeMemberName != null ? innerTypeMemberName : "<unknown member>")+ 
									"') cannot be aggregated, only members with array or Collection of primitive types can be aggregated.");
		}
		
		IStatisticInfoGenerator infoGenerator = getStatisticGenerator(statName);
		List<MemberInfo> parameters = new ArrayList<MemberInfo>();
		
		MemberInfo collectionMemberInfo = new MemberInfo(memberAccessor, memberType.getSimpleName(), memberType);
		collectionMemberInfo.setInnerType(innerType);
		
		if (!Util.isAcceptableSimpleType(innerType)){
			collectionMemberInfo = innerOperatorInfoGenerator.generateListInfoObject(collectionMemberInfo, new MemberInfo(innerTypeMemberName, innerTypeMemberType.getSimpleName(), innerTypeMemberType));
		}
		parameters.add(collectionMemberInfo);
		
		SimpleGeneratedMemberInfo scriptMemberInfo = infoGenerator.generateInfoObject(statName + serial++, parameters);
		scriptMemberInfo.setGeneratorName(statisticsNames.get(statName));
		
		List<MemberInfo> buildingBlocks = new ArrayList<MemberInfo>();
		buildingBlocks.add(collectionMemberInfo);
		scriptMemberInfo.addBuildingBlock(buildingBlocks);
		RecordableElement recordableElement = new RecordableElement(scriptMemberInfo, statName + "(" + annotationName + ")");
		RecordableInfo recordableInfo = InfoConverter.convertRecordableElement2RecordableInfo(recordableElement);
		
		String annotationValue = statName + "(" + annotationName + ")";
		String accessorName = memberAccessor;
		if (innerTypeMemberName != null && !innerTypeMemberName.isEmpty()){
			accessorName += "{" + innerType.getName() + " -> " + innerTypeMemberName + "}";
		}
		addUsedRecorderSource(annotationValue, accessorName);
		checkUsedRecorderSources(annotationValue);
		recorderInfo.getRecordables().add(recordableInfo);
		
		if (collectionMemberInfo instanceof InnerOperatorGeneratedMemberInfo){
			// we need to create an inner script
			Script innerScript = new Script();
			innerScript.setName(collectionMemberInfo.getName());
			innerScript.setScriptType(ScriptTypes.OPERATOR);
			innerScript.setInner(true);
			innerScript.setJavaType(collectionMemberInfo.getJavaType().getCanonicalName());
			innerScript.setType(collectionMemberInfo.getType());
			innerScript.setSource(((InnerOperatorGeneratedMemberInfo) collectionMemberInfo).getSource());
			innerScript.setDisplayName(((OperatorGeneratedMemberInfo) collectionMemberInfo).getDisplayName());
			innerScript.setReferences("");
			Parent parent = new Parent();
			parent.setName(memberAccessor);
			parent.setType(memberType.getSimpleName());
			parent.setJavaType(memberType.getName());
			parent.setInnerType(innerType.getName());
			innerScript.setParent(parent);
			recordersPage.getScripts().getScript().add(innerScript);
		}
		
		Script script = new Script();
		script.setName(scriptMemberInfo.getName());
		script.setScriptType(ScriptTypes.STAT);
		script.setCall(scriptMemberInfo.getCall());
		script.setInner(false); // TODO what should go here?
		script.setJavaType(scriptMemberInfo.getJavaType().getCanonicalName());
		script.setType(scriptMemberInfo.getType());
		script.setSource(scriptMemberInfo.getSource());
		if (collectionMemberInfo instanceof InnerOperatorGeneratedMemberInfo){
			script.setReferences(collectionMemberInfo.getName());
		} else {
			script.setReferences("");
		}
		script.setGeneratorName(scriptMemberInfo.getGeneratorName());
		BuildBlocks buildBlocks = new BuildBlocks();
		List<Block> xmlBlocksList = buildBlocks.getBlock();
		List<List<? extends Object>> blocksList = scriptMemberInfo.getBuildingBlocks();
		for (List<? extends Object> blocks : blocksList) {
			Block xmlBlock = new Block();
			xmlBlocksList.add(xmlBlock);
			List<JAXBElement<? extends BlockElement>> xmlBlockElements = xmlBlock.getBlockElement();
			for (Object object : blocks) {
				if (object instanceof MemberInfo) {
					MemberInfo memberInfo = (MemberInfo) object;
					
					BlockElementComplex xmlBlockElement = new BlockElementComplex();
					xmlBlockElement.setBlockElementType(BlockElementTypes.MEMBER);
					xmlBlockElement.setType(memberInfo.getType());
					Class<?> type = memberInfo.getInnerType();
					if (type != null){
						xmlBlockElement.setInnerType(type.getName());
					} else {
						xmlBlockElement.setInnerType("null");
					}
					xmlBlockElement.setJavaType(memberInfo.getJavaType().getCanonicalName());
					xmlBlockElement.setName(memberInfo.getName());
					
					
					xmlBlockElements.add(objFactory.createBlockElement(xmlBlockElement));
				}
			}
		}
		script.setBuildBlocks(buildBlocks);
		
		recordersPage.getScripts().getScript().add(script);
		
//		addUsedRecorderSource(annotationValue, memberAccessor);
//		checkUsedRecorderSources(annotationValue);

		
		Member member = new Member();
		member.setAlias(recordableElement.getAlias());
		member.setJavaType(scriptMemberInfo.getJavaType().getCanonicalName());
		member.setType(scriptMemberInfo.getType());
		member.setContent(scriptMemberInfo.getName());
		recorder.getMember().add(member);
	}

	protected void checkUsedRecorderSources(String annotationName) {
		List<String> usedRecordersList = usedRecorderSources.get(annotationName);
		if (usedRecordersList.size() > 1){
			StringBuffer stringBuffer = new StringBuffer("Multiple recorder sources under the name '" + annotationName+ "' (");
			for (String usedRecorder : usedRecordersList) {
				stringBuffer.append(usedRecorder).append(", ");
			}
			stringBuffer.delete(stringBuffer.length() - 2, stringBuffer.length());
			stringBuffer.append(")");
//			Logger.logWarning(stringBuffer.toString());
			throw new IllegalArgumentException(stringBuffer.toString());
		}
	}

	private static IStatisticInfoGenerator getStatisticGenerator(String statName) {
		try {
			IStatisticInfoGenerator infoGenerator = infoGenerators.get(statName);
			if (infoGenerator == null){
				Class<?> pluginClass = statisticsPluginNames.get(statName);
				if (pluginClass == null){ // if not in the list of supported plugins, we can still try to load a plugin with the given name
					StringBuilder sb = new StringBuilder(statName);
					sb.setCharAt(0, Character.toTitleCase(sb.charAt(0)));
					String className = "ai.aitia.meme.paramsweep.coltPlugin.Colt_" + sb.toString();
					pluginClass = Class.forName(className);
				}
				infoGenerator = new StatisticsInfoGenerator((IStatisticsPlugin) pluginClass.newInstance());
				infoGenerators.put(statName, infoGenerator);
				statisticsNames.put(statName, pluginClass.getName());
			}
			
			return infoGenerator;
		} catch (InstantiationException e) {
			throw new IllegalStateException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	protected void addUsedRecorderSource(final String annotationName, final String accessorName){
		List<String> accessorList = usedRecorderSources.get(annotationName);
		
		if (accessorList == null){
			accessorList = new ArrayList<String>();
			usedRecorderSources.put(annotationName, accessorList);
		}
		
		accessorList.add(accessorName);
	}
	
}
