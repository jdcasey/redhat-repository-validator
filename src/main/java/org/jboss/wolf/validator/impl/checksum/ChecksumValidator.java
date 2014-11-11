package org.jboss.wolf.validator.impl.checksum;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.jboss.wolf.validator.internal.Utils.relativize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.eclipse.aether.util.ChecksumUtils;
import org.jboss.wolf.validator.Validator;
import org.jboss.wolf.validator.ValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ChecksumValidator implements Validator {
    
    private static final Logger logger = LoggerFactory.getLogger(ChecksumValidator.class);

    private static final Map<String, String> checksumAlgorithms = new HashMap<String, String>();
    
    private static final List<String> excludedFiles = new ArrayList<String>();
    
    static {
        checksumAlgorithms.put("MD5", ".md5");
        checksumAlgorithms.put("SHA-1", ".sha1");

        excludedFiles.add(".maven-repository");
    }

    @Inject @Named("checksumValidatorFilter")
    private IOFileFilter fileFilter;

    @Override
    public void validate(ValidatorContext ctx) {
        Collection<File> files = findFiles(ctx);
        for (File file : files) {
            logger.trace("validating {}", relativize(ctx, file));
            validateChecksum(ctx, file);
        }
    }

    private void validateChecksum(ValidatorContext ctx, File file) {
        Map<String, Object> checksums;
        try {
            checksums = ChecksumUtils.calc(file, checksumAlgorithms.keySet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Entry<String, String> checksumAlgorithm : checksumAlgorithms.entrySet()) {
            try {
                String checksum1 = checksums.get(checksumAlgorithm.getKey()).toString();
                String checksum2 = ChecksumUtils.read(new File(file.getPath() + checksumAlgorithm.getValue()));
                if (!equalsIgnoreCase(checksum1, checksum2)) {
                    ctx.addError(this, file, new ChecksumNotMatchException(relativize(ctx, file), checksumAlgorithm.getKey(), checksum1, checksum2));
                }
            } catch (IOException e) {
                ctx.addError(this, file, new ChecksumNotExistException(relativize(ctx, file), checksumAlgorithm.getKey()));
            }
        }
    }

    private Collection<File> findFiles(ValidatorContext ctx) {
        IOFileFilter excludedChecksumFiles = notFileFilter(new NameFileFilter(excludedFiles));
        IOFileFilter filterChecksumFiles = notFileFilter(new SuffixFileFilter(new ArrayList<String>(checksumAlgorithms.values())));
        Collection<File> files = listFiles(ctx.getValidatedRepository(), and(fileFilter, filterChecksumFiles, excludedChecksumFiles), trueFileFilter());
        return files;
    }

}