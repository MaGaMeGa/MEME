package ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.BooleanGene;

import com.google.common.base.Preconditions;

public class IdentifiableBooleanGene extends BooleanGene implements IIdentifiableGene {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 805023356484580860L;
	
	protected final String id;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public IdentifiableBooleanGene(final String id) throws InvalidConfigurationException {
		Preconditions.checkNotNull(id);
		this.id = id;
	}

	//----------------------------------------------------------------------------------------------------
	public IdentifiableBooleanGene(final String id, final Configuration a_config) throws InvalidConfigurationException {
		super(a_config);
		Preconditions.checkNotNull(id);
		this.id = id;
	}

	//----------------------------------------------------------------------------------------------------
	public IdentifiableBooleanGene(final String id, final Configuration a_config, final boolean a_value) throws InvalidConfigurationException {
		super(a_config, a_value);
		Preconditions.checkNotNull(id);
		this.id = id;
	}

	//----------------------------------------------------------------------------------------------------
	public IdentifiableBooleanGene(final String id, final Configuration a_config, final Boolean a_value) throws InvalidConfigurationException {
		super(a_config, a_value);
		Preconditions.checkNotNull(id);
		this.id = id;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getId() { return id; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		String s = "IdentifiableBooleanGene" + "=";
		if (getInternalValue() == null) {
			s += "null";
		} else {
			s += getInternalValue().toString();
		}
		return s;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object a_other) {
		if (a_other instanceof IdentifiableBooleanGene) {
			final IdentifiableBooleanGene that = (IdentifiableBooleanGene) a_other;
			return this.id.equals(that.id) && super.equals(that);
		}
		
		return false;
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	protected Gene newGeneInternal() {
		try {
			final IdentifiableBooleanGene result = new IdentifiableBooleanGene(id,getConfiguration());
			return result;
		} catch (final InvalidConfigurationException iex) {
			throw new IllegalStateException(iex.getMessage());
		}
	}
}
