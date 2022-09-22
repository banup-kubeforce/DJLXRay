package com.example.djlxray;

import ai.djl.MalformedModelException;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.SingleShotDetectionTranslator;
import ai.djl.repository.zoo.ModelNotFoundException;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
@SpringBootApplication
public class DjlxRayApplication {
    private static final Logger logger = LoggerFactory.getLogger(DjlxRayApplication.class);
    private static final List<String> CLASSES = Arrays.asList("Normal", "Pneumonia");
    public static void main(String[] args) throws IOException, ModelNotFoundException, MalformedModelException {
        SpringApplication.run(DjlxRayApplication.class, args);
        String imagePath;
        if (args.length == 0) {
            imagePath = "https://djl-ai.s3.amazonaws.com/resources/images/chest_xray.jpg";
            logger.info("Input image not specified, using image:\n\t{}", imagePath);
        } else {
            imagePath = args[0];
        }
        Image image;
        if (imagePath.startsWith("http")) {
            image = ImageFactory.getInstance().fromUrl(imagePath);
        } else {
            image = ImageFactory.getInstance().fromFile(Paths.get(imagePath));
        }


        Translator<Image, Classifications> translator =
                ImageClassificationTranslator.builder()
                        .addTransform(a -> NDImageUtils.resize(a, 224).div(255.0f))
                        .optSynset(CLASSES)
                        .build();
        Criteria<Image, Classifications> criteria =
                Criteria.builder()
                        .setTypes(Image.class, Classifications.class)
                        .optModelUrls("https://djl-ai.s3.amazonaws.com/resources/demo/pneumonia-detection-model/saved_model.zip")
                        .optTranslator(translator)
                        .build();

        try (ZooModel<Image, Classifications> model = criteria.loadModel();
             Predictor<Image, Classifications> predictor = model.newPredictor()) {
            Classifications result = predictor.predict(image);
            logger.info("Diagnose: {}", result);
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        }
    }

    }


