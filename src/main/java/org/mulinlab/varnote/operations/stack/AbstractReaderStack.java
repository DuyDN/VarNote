package org.mulinlab.varnote.operations.stack;

import java.util.List;

import org.mulinlab.varnote.operations.process.ProcessResult;
import org.mulinlab.varnote.operations.readers.db.AbstractDBReader.Iterator;
import org.mulinlab.varnote.utils.node.LocFeature;

public abstract class AbstractReaderStack{
	
	protected Iterator it = null;
	protected ProcessResult resultProcessor;
	protected boolean iseof;
	
	public AbstractReaderStack(ProcessResult resultProcessor) {
		super();
		this.resultProcessor = resultProcessor;
		iseof = false;
	}	
	
	public void setResultProcessor(final ProcessResult resultProcessor) {
		this.resultProcessor = resultProcessor;
	}

	public void setIterator(final Iterator it) {
		this.it = it;
		iseof = false;
	}
	
	public void clearST() {
	}
	
	public void setIseof(boolean iseof) {
		this.iseof = iseof;
	}

	public Iterator getIt() {
		return it;
	}

	public ProcessResult getResultProcessor() {
		return resultProcessor;
	}

	public abstract void findOverlaps(List<LocFeature> nodes);
	public abstract void findOverlap(LocFeature node);
	public abstract boolean findOverlapInST(LocFeature query) ;
}
