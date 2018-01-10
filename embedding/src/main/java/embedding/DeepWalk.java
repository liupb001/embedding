package embedding;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.FileSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
//import org.nd4j.jita.conf.CudaEnvironment;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.util.DataTypeUtil;
//import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Learn deep walk embedding on the sample sequences.
 *
 * Created by kok on 1/2/17.
 */
public class DeepWalk {
    public static int Year = 2013;
    private static Logger log = LoggerFactory.getLogger(DeepWalk.class);

    public static void learnEmbedding() throws Exception {
        learnEmbedding("tract", "usespatial");
    }

    public static void learnEmbedding(String regionLevel, String spatialGF) throws Exception{
//        DataTypeUtil.setDTypeForContext(DataBuffer.Type.HALF);
//        Nd4j.create(32);
//
//        CudaEnvironment.getInstance().getConfiguration()
//                // key option enabled
//                .allowMultiGPU(false)
//
//                // we're allowing larger memory caches
//                .setMaximumDeviceCache(4L * 1024L * 1024L * 1024L)
//
//                // cross-device access is used for faster model averaging over pcie
//                .allowCrossDeviceAccess(false);


        SentenceIterator itr;
        if (spatialGF.equals("usespatial")) {
            File seqDir = new File(String.format("../miscs/%d/deepwalkseq-%s", Year, regionLevel));
            itr = new FileSentenceIterator(seqDir);
        } else if (spatialGF.equals("nospatial")) {
            File seqFile = new File(String.format("../miscs/%d/deepwalkseq-%s/taxi-crosstime.seq", Year, regionLevel));
            itr = new LineSentenceIterator(seqFile);
        } else if (spatialGF.equals("onlyspatial")) {
            File seqFile = new File(String.format("../miscs/%d/deepwalkseq-%s/taxi-spatial.seq", Year, regionLevel));
            itr = new LineSentenceIterator(seqFile);
        }else {
            itr = null;
        }

        String out = String.format("../miscs/%d/taxi-deepwalk-%s-%s-2D.vec", Year, regionLevel, spatialGF);
        int layerSize = 20;
        if (regionLevel.equals("CA"))
            layerSize = 2;
        else if (regionLevel.equals("tract"))
            layerSize = 20;

        log.info("Load and vectorize");

        TokenizerFactory t = new DefaultTokenizerFactory();

        log.info("Building model");
        Word2Vec w2v = new Word2Vec.Builder().minWordFrequency(2)
                .layerSize(layerSize).iterations(1).windowSize(LayeredGraph.numLayer)
                .negativeSample(5).workers(8)
                .iterate(itr).tokenizerFactory(t).build();

        log.info("Fitting w2v model");
        w2v.fit();

        log.info("Writing w2v vectors into files");
        WordVectorSerializer.writeWordVectors(w2v, out);
    }

    public static void checkInputFile(String regionLevel, String spatialGF) {
        File sgSeq = new File(String.format("../miscs/%d/deepwalkseq-%s/taxi-spatial.seq", Year, regionLevel));
        if ((spatialGF.equals("usespatial") || spatialGF.equals("onlyspatial")) && !sgSeq.exists()) {
            System.out.format("The spatial graph samples for %s do not exist, but we need it! Generating ...\n", regionLevel);
            if (regionLevel.equals("tract")) {
                SpatialGraph.numSamples = 600_000;
                SpatialGraph.numLayer = 8;
            } else {
                SpatialGraph.numSamples = 80_000;
                SpatialGraph.numLayer = 24;
            }
            SpatialGraph.outputSampleSequence(regionLevel);
        }

        File ctSeq = new File(String.format("../miscs/%d/deepwalkseq-%s/taxi-crosstime.seq", Year, regionLevel));
        if ((spatialGF.equals("nospatial") || spatialGF.equals("usespatial")) && !ctSeq.exists()) {
            System.out.format("The transition graph samples for %s do not exists! Generating ...\n", regionLevel);
            if (regionLevel.equals("tract")) {
                CrossTimeGraph.numSamples = 15_000_000;
                CrossTimeGraph.numLayer = 8;
            } else {
                CrossTimeGraph.numSamples = 8_000_000;
                CrossTimeGraph.numLayer = 24;
            }
//            int[] intervals = new int[]{22, 5, 11, 16, 22};
            CrossTimeGraph.outputSampleSequence(regionLevel);
        }
    }

    /**
     * Train deepwalk on given graph.
     * @param argv
     *  argv[0] is either "tract" or "CA", which defines the regionLevel
     *  argv[1] is either "usespatial" or "nospatial", which defines the spatial graph flag.
     */
    public static void main(String[] argv) {
        try {
            String regionLevel = "tract";
            String spatialGF = "usespatial";
            if (argv.length > 0) {
                regionLevel = argv[0];
                System.out.format("word2vec learn embedding at %s level.\n", regionLevel);
            }
            if (argv.length > 1) {
                spatialGF = argv[1];
                System.out.format("Spatial graph use or not: %s.\n", spatialGF);
            }
            if (argv.length > 2) {
                DeepWalk.Year = Integer.parseInt(argv[2]);
            }
            checkInputFile(regionLevel, spatialGF);
            learnEmbedding(regionLevel, spatialGF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
