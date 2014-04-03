/**
 * 
 */
package ai.aitia.meme.paramsweep.generator;

import java.util.ArrayList;

import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.InnerOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;

/**
 * @author Tamás Máhr
 *
 */
public class InnerOperatorsInfoGenerator {

	private static int generatedIdx = 0;

	/**
	 * Creates an InnerOperatorGeneratedMemberInfo object for collecting values from a collection or an array.
	 * 
	 * @param object the info of the collection or array whose elements should be queried 
	 * @param member the info of the member of the elements to be queried
	 * @return a freshly created InnerOperatorGeneratedMemberInfo
	 */
	public InnerOperatorGeneratedMemberInfo generateListInfoObject(final MemberInfo object, final MemberInfo member) {
		InnerOperatorGeneratedMemberInfo newInfo = new InnerOperatorGeneratedMemberInfo("innerList" + generatedIdx++ + "()",ArrayList.class.getSimpleName(),ArrayList.class,object);
		if (object instanceof GeneratedMemberInfo)
			newInfo.addReference((GeneratedMemberInfo)object);
		newInfo.setDisplayName(Utilities.name(object) + "." + Utilities.name(member) + "[]");
		String accessor = Util.GENERATED_MODEL_MODEL_FIELD_NAME + "." + object.getName();
		StringBuilder code = new StringBuilder();
		Class<?> innerType = object.getInnerType();
		if (Utilities.isPrimitive(member.getJavaType())) {
			code.append("java.util.ArrayList result = new java.util.ArrayList(" + accessor);
			if (object.getType().endsWith("[]")) {
				code.append(".length);\n");
				code.append("for (int i = 0;i < " + accessor + ".length;++i) {\n");
				code.append(Util.boxingIfNeed(accessor + "[i]." + member.getName(),member.getJavaType()));
				code.append("}\n");
			} else {
				code.append(".size());\n");
				code.append("Object[] source = " + accessor + ".toArray();\n");
				code.append("for (int i = 0;i < source.length;++i) {\n");
				code.append(Util.boxingIfNeed("((" + innerType.getName() + ")source[i])." + member.getName(),member.getJavaType()));
				code.append("}\n");
			}
			code.append("return result;\n");
			newInfo.setInnerType(Util.boxingType(member.getJavaType()));
		} else {
			code.append("java.util.ArrayList result = new java.util.ArrayList();\n");
			if (object.getType().endsWith("[]")) {
				code.append("for (int i = 0;i < " + accessor + ".length;++i) {\n");
				code.append(member.getJavaType().getCanonicalName() + " temp = " + accessor + "[i]." + member.getName() + ";\n");
				code.append("for (int j = 0;j < temp.length;++j) {\n");
				code.append(Util.boxingIfNeed("temp[j]",member.getInnerType()));
				code.append("}\n}\n");
			} else {
				code.append("Object[] source = " + accessor + ".toArray();\n");
				code.append("for (int i = 0;i < source.length;++i) {\n");
				code.append(member.getJavaType().getCanonicalName() + " temp = ((" + innerType.getName() + ")source[i])." + member.getName() + ";\n");
				code.append("for (int j = 0;j < temp.length;++j) {\n");
				code.append(Util.boxingIfNeed("temp[j]",member.getInnerType()));
				code.append("}\n}\n");
			}
			code.append("return result;\n");
			newInfo.setInnerType(Util.boxingType(member.getInnerType()));
		}
		newInfo.setSource(code.toString());
		return newInfo;
	}

	/**
	 * Creates an InnerOperatorGeneratedMemberInfo object for collecting values from a simple member.
	 * 
	 * @param object the info of the collection or array whose elements should be queried 
	 * @param member the info of the member of the elements to be queried
	 * @return a freshly created InnerOperatorGeneratedMemberInfo
	 */
	public InnerOperatorGeneratedMemberInfo generateSimpleInfoObject(final MemberInfo object, final MemberInfo member){
		InnerOperatorGeneratedMemberInfo newInfo = new InnerOperatorGeneratedMemberInfo("innerSelection" + generatedIdx++ + "()",member.getJavaType().getSimpleName(),member.getJavaType(),object);
		String accessor = Util.GENERATED_MODEL_MODEL_FIELD_NAME + "." + object.getName();
		newInfo.setDisplayName(Utilities.name(object) + "." + Utilities.name(member));
		newInfo.setSource("return " + accessor + "." + member.getName() + ";\n");
		if (object instanceof GeneratedMemberInfo)
			newInfo.addReference((GeneratedMemberInfo)object);

		return newInfo;
	}

}
