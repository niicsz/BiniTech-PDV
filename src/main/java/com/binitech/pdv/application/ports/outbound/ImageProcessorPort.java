package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.StoredImage;
import com.binitech.pdv.utils.enums.ImageKind;

public interface ImageProcessorPort {

  StoredImage process(byte[] content, ImageKind kind);
}
