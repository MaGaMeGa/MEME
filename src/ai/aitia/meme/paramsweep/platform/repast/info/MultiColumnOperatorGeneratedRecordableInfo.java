/**
 * 
 */
package ai.aitia.meme.paramsweep.platform.repast.info;

/**
 * @author Tamás Máhr
 *
 */
public class MultiColumnOperatorGeneratedRecordableInfo extends OperatorGeneratedRecordableInfo {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	/**
	 * The name of the method that returns the length of the collection to be recorded.
	 */
	protected String collectionLength;
	
	/**
	 * @param name
	 * @param type
	 * @param accessibleName
	 * @param source
	 */
	public MultiColumnOperatorGeneratedRecordableInfo(String name, Class type, String accessibleName, String source, String collectionLength) {
		super(name, type, accessibleName, source);
		
		this.collectionLength = collectionLength;
	}

	/**
	 * @return the collectionLength
	 */
	public String getCollectionLength() {
		return collectionLength;
	}

	/**
	 * @param collectionLength the collectionLength to set
	 */
	public void setCollectionLength(String collectionLength) {
		this.collectionLength = collectionLength;
	}

}
