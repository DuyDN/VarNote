package org.mulinlab.varnote.utils.format;


import java.io.File;
import htsjdk.samtools.util.StringUtil;
import htsjdk.tribble.index.tabix.TabixFormat;
import org.mulinlab.varnote.constants.GlobalParameter;
import htsjdk.samtools.util.IOUtil;
import org.mulinlab.varnote.utils.database.index.TbiIndex;
import org.mulinlab.varnote.utils.enumset.Delimiter;
import org.mulinlab.varnote.utils.enumset.FormatType;
import org.mulinlab.varnote.utils.enumset.IndexType;
import org.mulinlab.varnote.exceptions.InvalidArgumentException;
import org.mulinlab.varnote.utils.VannoUtils;

public class Format extends TabixFormat {
	public final static String DEFAULT_COMMENT_INDICATOR = GlobalParameter.DEFAULT_COMMENT_INDICATOR;
	public final static int MAX_HEADER_COMPARE_LENGTH = 128;
	public final static int START_COMPARE_LENGTH = 7;
	public final static int NO_COL = -1;
	public final static int DEFAULT_RSID_COL = 2;

	public int refPositionColumn;
	public int altPositionColumn;
	public int rsidPositionColumn = NO_COL;

	private String extHeaderPath;
	private String commentIndicator;
	private boolean hasHeaderInFile;

	private String[] headerPart;
	public FormatType type;

	private String headerStr;
	private String headerStart;
	private String dataStr;
	private Delimiter delimiter = Delimiter.TAB;

	private boolean allowLargeVariants;
	private int maxVariantLength = GlobalParameter.DEFAULT_MAX_LENGTH;

//	public static Format newRSID() {
//		return new Format(GENERIC_FLAGS, 1, 2, 2, 0, DEFAULT_COMMENT_INDICATOR, NO_COL, NO_COL, false, FormatType.RSID);
//	}

	public static Format newTAB() {
		return new Format(GENERIC_FLAGS, NO_COL, NO_COL, NO_COL, 0, DEFAULT_COMMENT_INDICATOR, NO_COL, NO_COL, false, FormatType.TAB);
	}

	public static Format newBED() {
		return new Format(UCSC_FLAGS, 1, 2, 3, 0, DEFAULT_COMMENT_INDICATOR, NO_COL, NO_COL, false, FormatType.BED);
	}

	public static Format newVCF() {
		return new Format(VCF_FLAGS, 1, 2, 0, 0, DEFAULT_COMMENT_INDICATOR, 4, 5, true, FormatType.VCF);
	}

	public Format(final int flags, int sequenceColumn, int startPositionColumn, int endPositionColumn, final int numHeaderLinesToSkip,
				  final String commentIndicator, int refColumn, int altColumn, boolean hasHeaderInFile) {
		super(flags, sequenceColumn, startPositionColumn, endPositionColumn, '#', numHeaderLinesToSkip);

		this.refPositionColumn = refColumn;
		this.altPositionColumn = altColumn;
		this.commentIndicator = commentIndicator;
		this.hasHeaderInFile = hasHeaderInFile;

		if(flags == VCF_FLAGS) {
			type = FormatType.VCF;
		} else if(flags == UCSC_FLAGS && sequenceColumn == 1 && startPositionColumn == 2 && endPositionColumn == 3) {
			type = FormatType.BED;
		} else if(flags == UCSC_FLAGS && sequenceColumn == 1 && startPositionColumn == 2 && endPositionColumn == 3 && refPositionColumn == 4 && altPositionColumn == 5) {
			type = FormatType.BEDALLELE;
		} else {
			type = FormatType.TAB;
		}
	}
	
	public Format(final int flags, int sequenceColumn, int startPositionColumn, int endPositionColumn, final int numHeaderLinesToSkip,
		final String commentIndicator, int refColumn, int altColumn, boolean hasHeader, FormatType type) {
		this(flags, sequenceColumn, startPositionColumn, endPositionColumn, numHeaderLinesToSkip, commentIndicator, refColumn, altColumn, hasHeader);
		this.type = type;
    }
	
	public static Format defaultFormat(final String input, final boolean isQuery) {
		Format defaultFormat;

		defaultFormat = VannoUtils.determineFileType(input);
		if(defaultFormat == null) {
			if(isQuery) {
				return Format.newTAB();
			} else {
				File tbiIndex = new File(input + IndexType.TBI.getExtIndex());
				if(tbiIndex.exists()) {
					return new TbiIndex(tbiIndex.getPath()).getFormat();
				} else {
					return Format.newTAB();
				}
			}
		}
		return defaultFormat;
	}

	@Override
	public String toString() {
		return String.format("%d,%d,%d,%d,%d,%s,%d,%d,%s", flags, sequenceColumn, startPositionColumn, endPositionColumn, numHeaderLinesToSkip, commentIndicator,
				refPositionColumn, altPositionColumn, hasHeaderInFile);
	}

	public String logFormat() {
		if(this.type == FormatType.VCF) {
			return "VCF";
		} else if(this.type == FormatType.BED) {
			return "BED-Like";
		} else if(this.type == FormatType.BEDALLELE) {
			return "BED-Like Allele";
		} else if(this.type == FormatType.RSID) {
			return "RSID";
		} else {
			return String.format("TAB, CHROM:%s BEGIN:%s END:%s REF:%s ALT:%s ZeroBased:%s", sequenceColumn, startPositionColumn, endPositionColumn,
					refPositionColumn == NO_COL ? '-' : refPositionColumn + "",
					altPositionColumn == NO_COL ? '-' : altPositionColumn + "", isZeroBased());
		}
	}
	
	public static Format readFormatString(final String str) {
		String[] cols = str.split(",");
		if(cols.length != 9) throw new InvalidArgumentException("Parsing format with error, 9 columns are expected.");

		return new Format(Integer.parseInt(cols[0]), Integer.parseInt(cols[1]), Integer.parseInt(cols[2]), Integer.parseInt(cols[3]), Integer.parseInt(cols[4]),
				cols[5], Integer.parseInt(cols[6]), Integer.parseInt(cols[7]), VannoUtils.strToBool(cols[8]));
	}

	public String getCommentIndicator() {
		return commentIndicator;
	}

	public boolean isHasHeader() {
		return hasHeaderInFile;
	}

	public void setHasHeaderInFile(boolean hasHeaderInFile) {
		this.hasHeaderInFile = hasHeaderInFile;
	}

	public String getHeaderPath() {
		return extHeaderPath;
	}

	public void setHeaderPath(String headerPath) {
		IOUtil.assertInputIsValid(headerPath);
		this.extHeaderPath = VannoUtils.getAbsolutePath(headerPath);
		this.setHasHeaderInFile(false);
	}

	public boolean isRefAndAltExsit() {
		return (refPositionColumn > 0) && (altPositionColumn > 0);
	}

	public boolean hasLoc() {
		return (sequenceColumn > 0) && (startPositionColumn > 0) && (endPositionColumn > -1);
	}

	public void checkLoc() {
		if(this.type != FormatType.RSID) {
			if (sequenceColumn < 1)
				throw new InvalidArgumentException("c of sequence name is expected by -c");
			if (startPositionColumn < 1)
				throw new InvalidArgumentException("Valid column of start chromosomal position is expected by -b");
			if (endPositionColumn < 0)
				throw new InvalidArgumentException("Valid column of end chromosomal position is expected by -e");
		}

		if(endPositionColumn == 0) {
			endPositionColumn = startPositionColumn;
		}
	}

	public void setZeroBased() {
		if(this.flags == GENERIC_FLAGS) {
			this.flags = GENERIC_FLAGS | ZERO_BASED;
		}
	}
	
	public boolean isZeroBased() {
		return ((this.flags & 0x10000) != 0 );
	}
	
	public void setCommentIndicator(String commentIndicator) {
		if((commentIndicator == null) || commentIndicator.equals(""))  return;
		this.commentIndicator = VannoUtils.replaceQuote(commentIndicator.trim());
	}

	public int getFlags() {
		return flags;
	}

	public FormatType getType() {
		return type;
	}

	public String[] getHeaderPart() {
		return headerPart;
	}

	public String getHeaderPartStr() {
		return StringUtil.join(getDelimStr(), headerPart);
	}

	public int getHeaderPartSize() {
		return headerPart.length;
	}

	public String getColumnName(final int col) {
		return headerPart[col - 1];
	}

	public void setHeaderPart(final String[] headerPart) {
		this.headerPart = headerPart;
	}

	public void setHeader(String header) {
		this.headerStr = header.substring(0, (header.length() > MAX_HEADER_COMPARE_LENGTH) ? MAX_HEADER_COMPARE_LENGTH : header.length());
		this.headerStart = header.substring(0, (header.length() > START_COMPARE_LENGTH) ? START_COMPARE_LENGTH : header.length());
	}

	public String getHeaderStr() {
		return headerStr;
	}

	public String getHeaderStart() {
		return headerStart;
	}

	public boolean isPos() {
		return ((endPositionColumn == 0) || (endPositionColumn == startPositionColumn));
	}

	public String getDataStr() {
		return dataStr;
	}

	public void setDataStr(final String dataStr) {
		this.dataStr = dataStr;
	}

	public int getCol(String str) {
		str = str.toUpperCase();
		for (int i = 0; i < headerPart.length; i++) {
			if(headerPart[i].toUpperCase().equals(str)) {
				return i+1;
			}
		}
		return -1;
	}

	public void setDelimiter(Delimiter delimiter) {
		this.delimiter = delimiter;
	}

	public Delimiter getDelimiter() {
		return delimiter;
	}

	public char getDelimChar() {
		return delimiter.getC();
	}

	public String getDelimStr() {
		return delimiter.getCStr();
	}

	public boolean isAllowLargeVariants() {
		return allowLargeVariants;
	}

	public void setAllowLargeVariants(boolean allowLargeVariants) {
		this.allowLargeVariants = allowLargeVariants;
	}

	public int getMaxVariantLength() {
		return maxVariantLength;
	}

	public void setMaxVariantLength(int maxVariantLength) {
		this.maxVariantLength = maxVariantLength;
	}
}
