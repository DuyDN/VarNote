package org.mulinlab.varnote.operations.readers.query;

import htsjdk.samtools.util.IOUtil;
import org.mulinlab.varnote.filters.RegionFilter;
import org.mulinlab.varnote.filters.iterator.LocFilterIterator;
import org.mulinlab.varnote.filters.query.VariantFilter;
import org.mulinlab.varnote.utils.headerparser.HeaderFormatReader;
import org.mulinlab.varnote.filters.query.line.LineFilter;
import org.mulinlab.varnote.operations.readers.itf.*;
import org.mulinlab.varnote.filters.iterator.LineFilterIterator;
import org.mulinlab.varnote.utils.VannoUtils;
import org.mulinlab.varnote.utils.enumset.FileType;
import org.mulinlab.varnote.utils.format.Format;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFileReader {

    protected List<LineFilter> lineFilters;

    protected String filePath;
    protected FileType fileType;
    protected QueryReaderItf reader;
    protected Format format;

    protected LineFilterIterator iterator;
    protected boolean decodeFull = false;
    protected List<VariantFilter> locFilters;

    public AbstractFileReader() {
    }

    public AbstractFileReader(final QueryReaderItf itf, final Format format) {
        this.reader = itf;

        format.checkLoc();
        this.format = HeaderFormatReader.readHeader(format, getFilePath(), getFileType());

        this.lineFilters = new ArrayList<>();
        this.initLineFilters();
    }

    public AbstractFileReader(final String path, final Format format) {
        this(path, VannoUtils.checkFileType(path), format);
    }

    public AbstractFileReader(final String path, final FileType fileType, final Format format) {
        this(getReader(path, fileType), format);
        this.filePath = path;
        this.fileType = fileType;
    }

    protected abstract void initLineFilters();
    public abstract LineFilterIterator getFilterIterator();
    public void addLineFilters(final LineFilter lineFilter) {
        lineFilters.add(lineFilter);
    }
    public Format getFormat() {
        return format;
    }
    public void setDecodeFull(final boolean decodeFull) {
        this.decodeFull = decodeFull;
    }

    public static QueryReaderItf getReader(final String path) {
        return getReader(path, VannoUtils.checkFileType(path));
    }

    public String getFilePath() {
        if(filePath == null) filePath = reader.getFilePath();
        return filePath;
    }

    public FileType getFileType() {
        if(fileType == null) fileType = VannoUtils.checkFileType(getFilePath());
        return fileType;
    }

    public static QueryReaderItf getReader(final String path, final FileType fileType) {
        IOUtil.assertInputIsValid(path);
        try {
            if (fileType == FileType.BGZ) {
                return new BGZReader(path);
            } else if (fileType == FileType.GZ) {
                return new GZIPReader(path);
            } else {
                return new LongLineReader(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isDecodeFull() {
        return decodeFull;
    }

    public void setLocFilters(List<VariantFilter> locFilters) {
        this.locFilters = locFilters;
    }

    public void setLocFilter() {
        if(iterator != null && locFilters != null) {
            ((LocFilterIterator)iterator).setLocFilters(locFilters);
        }
    }
}
