package edu.njupt.flowanalysis;

import de.ecspride.sourcesinkfinder.CutoffClassifier;
import de.ecspride.sourcesinkfinder.IFeature;
import de.ecspride.sourcesinkfinder.SourceSinkFinder;
import de.ecspride.sourcesinkfinder.features.AbstractSootFeature;
import edu.njupt.flowanalysis.managers.FileManager;
import net.dongliu.apk.parser.ApkFile;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.options.Options;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.rules.JRip;
import weka.classifiers.trees.J48;
import weka.core.*;
import weka.core.converters.ArffSaver;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SourceSinkParser extends SourceSinkFinder{
    private String modelPath = null;
    private String jarPath;
    private String resultFile;

    /**
     *
     * @param resultFile : output sources and sinks
     */
    public Set<AndroidMethod> loadApkMethods(String jarPath, String apkFile, String resultFile) throws Exception {
        // apk->test instances
        // soot configuration
        soot.G.reset();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_dir(Collections.singletonList(apkFile));
        Options.v().set_force_android_jar(jarPath);
        Options.v().set_prepend_classpath(true);
        Options.v().set_keep_offset(false);
        Options.v().set_ignore_resolution_errors(true);
        Scene.v().loadNecessaryClasses();

        // get all methods
        Set<AndroidMethod> methods = new HashSet<>();
        for (String clzName : SourceLocator.v().getClassesUnder(apkFile)) {
            System.out.printf("api class: %s\n", clzName);
            // 加载要处理的类设置为应用类，并加载到soot环境Scene中
            SootClass sootClass = Scene.v().loadClass(clzName, SootClass.BODIES);
            sootClass.setApplicationClass();
        }
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (!sootClass.isInterface()
                    && !sootClass.isPrivate()) {
//							&& sc.getName().startsWith("android.")
//							&& sc.getName().startsWith("com."))
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.isConcrete()
                            && !method.isPrivate()) {
                        methods.add(new AndroidMethod(method));
                    }
                }
            }
        }

        return methods;
    }

    /**
     * get APK packageName
     * @param apkFilePath
     * @return
     */
    public static String getPackageName(String apkFilePath) {
        try {
            ApkFile apkFile = new ApkFile(new File(apkFilePath));
            return apkFile.getApkMeta().getPackageName();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void analyzeSourceSinkWeka(Set<AndroidMethod> methods, String targetFileName) throws IOException {
        FastVector ordinal = new FastVector();
        ordinal.addElement("true");
        ordinal.addElement("false");

        FastVector classes = new FastVector();
        classes.addElement("source");
        classes.addElement("sink");
        classes.addElement("neithernor");

        // Collect all attributes and create the instance set
        Map<IFeature, Attribute> featureAttribs = new HashMap<IFeature, Attribute>(this.featuresSourceSink.size());
        FastVector attributes = new FastVector();
        for (IFeature f : this.featuresSourceSink) {
            Attribute attr = new Attribute(f.toString(), ordinal);
            featureAttribs.put(f, attr);
            attributes.addElement(attr);
        }
        Attribute classAttr = new Attribute("class", classes);

        FastVector methodStrings = new FastVector();
        for (AndroidMethod am : methods)
            methodStrings.addElement(am.getSignature());
        attributes.addElement(classAttr);
        Attribute idAttr = new Attribute("id", methodStrings);
        attributes.addElement(idAttr);

        Instances trainInstances = new Instances("trainingmethods", attributes, 0);
        Instances testInstances = new Instances("allmethods", attributes, 0);
        trainInstances.setClass(classAttr);
        testInstances.setClass(classAttr);

        // Create one instance object per data row
        int sourceTraining = 0;
        int sinkTraining = 0;
        int nnTraining = 0;
        int instanceId = 0;
        Map<String, AndroidMethod> instanceMethods = new HashMap<String, AndroidMethod>(methods.size());
        Map<Integer, AndroidMethod> instanceIndices = new HashMap<Integer, AndroidMethod>(methods.size());
        for (AndroidMethod am : methods) {
            Instance inst = new Instance(attributes.size());
            inst.setDataset(trainInstances);

            for (Map.Entry<IFeature, Attribute> entry : featureAttribs.entrySet()){
                switch(entry.getKey().applies(am)){
                    case TRUE: inst.setValue(entry.getValue(), "true"); break;
                    case FALSE: inst.setValue(entry.getValue(), "false"); break;
                    default: inst.setMissing(entry.getValue());
                }
            }
            inst.setValue(idAttr, am.getSignature());
            instanceMethods.put(am.getSignature(), am);
            instanceIndices.put(instanceId++, am);

            // Set the known classifications
            if (am.isSource()) {
                inst.setClassValue("source");
                sourceTraining++;
            }
            else if (am.isSink()) {
                inst.setClassValue("sink");
                sinkTraining++;
            }
            else if (am.isNeitherNor()) {
                inst.setClassValue("neithernor");
                nnTraining++;
            }
            else
                inst.setClassMissing();

            if (am.isAnnotated())
                trainInstances.add(inst);
            else
                testInstances.add(inst);
        }

        try {
//			instances.randomize(new Random(1337));
            Classifier classifier = null;
            if(WEKA_LEARNER_ALL.equals("BayesNet"))			// (IBK / kNN) vs. SMO vs. (J48 vs. JRIP) vs. NaiveBayes // MultiClassClassifier f黵 ClassifierPerformanceEvaluator
                classifier = new BayesNet();
            else if(WEKA_LEARNER_ALL.equals("NaiveBayes"))
                classifier = new NaiveBayes();
            else if(WEKA_LEARNER_ALL.equals("J48"))
                classifier = new J48();
            else if(WEKA_LEARNER_ALL.equals("SMO"))
                classifier = new SMO();
            else if(WEKA_LEARNER_ALL.equals("JRip"))
                classifier = new JRip();
            else
                throw new Exception("Wrong WEKA learner!");

            ArffSaver saver = new ArffSaver();
            saver.setInstances(trainInstances);
            saver.setFile(new File("SourcesSinks_Train.arff"));
            saver.writeBatch();

            Evaluation eval = new Evaluation(trainInstances);
            StringBuffer sb = new StringBuffer();
            int numFolds = Math.min(trainInstances.numInstances(), 10);
            eval.crossValidateModel(classifier, trainInstances, numFolds, new Random(1337), sb, new Range(attributes.indexOf(idAttr) + 1 + ""/* "1-" + (attributes.size() - 1)*/), true);
            System.out.println(sb.toString());
            System.out.println("Class details: " + eval.toClassDetailsString());
            System.out.println("Ran on a training set of " + sourceTraining + " sources, "
                    + sinkTraining + " sinks, and " + nnTraining + " neither-nors");

            classifier.buildClassifier(trainInstances);
            weka.core.SerializationHelper.write(modelPath + "/SourceSinkModel.model", classifier);
            if(WEKA_LEARNER_ALL.equals("J48")){
                System.out.println(((J48)(classifier)).graph());
            }
            for (int instIdx = 0; instIdx < testInstances.numInstances(); instIdx++) {
                Instance inst = testInstances.instance(instIdx);
                assert inst.classIsMissing();
                AndroidMethod meth = instanceMethods.get(inst.stringValue(idAttr));
                double d = classifier.classifyInstance(inst);
                String cName = testInstances.classAttribute().value((int) d);
                if (cName.equals("source")) {
                    inst.setClassValue("source");
                    meth.setSource(true);
                }
                else if (cName.equals("sink")) {
                    inst.setClassValue("sink");
                    meth.setSink(true);
                }
                else if (cName.equals("neithernor")) {
                    inst.setClassValue("neithernor");
                    meth.setNeitherNor(true);
                }
                else
                    System.err.println("Unknown class name");
            }
        }
        catch (Exception ex) {
            System.err.println("Something went all wonky: " + ex);
            ex.printStackTrace();
        }

        if(DIFF)
            writeResultsToFiles(targetFileName, methods, true);
        else
            writeResultsToFiles(targetFileName, methods, false);

        Runtime.getRuntime().gc();
    }

    @Override
    protected void analyzeCategories(Set<AndroidMethod> methods, String targetFileName, boolean sources, boolean sinks) throws IOException {
        FastVector ordinal = new FastVector();
        ordinal.addElement("true");
        ordinal.addElement("false");

        // We are only interested in sources and sinks
        {
            Set<AndroidMethod> newMethods = new HashSet<AndroidMethod>(methods.size());
            for (AndroidMethod am : methods) {
                // Make sure that we run after source/sink classification
                assert am.isAnnotated();
                if (am.isSink() == sinks && am.isSource() == sources)
                    newMethods.add(am);
            }
            methods = newMethods;
        }
        System.out.println("We have a set of " + methods.size() + " sources and sinks.");

        // Build the class attribute, one possibility for every category
        FastVector classes = new FastVector();
        for (AndroidMethod.CATEGORY cat : AndroidMethod.CATEGORY.values()) {
            // Only add the class if it is actually used
            if (cat == AndroidMethod.CATEGORY.NO_CATEGORY)
                classes.addElement(cat.toString());
            else {
                for (AndroidMethod am : methods)
                    if (am.isSource() == sources
                            && am.isSink() == sinks
                            && am.getCategory() == cat) {
                        classes.addElement(cat.toString());
                        break;
                    }
            }
        }

        // Collect all attributes and create the instance set
        Map<IFeature, Attribute> featureAttribs = new HashMap<IFeature, Attribute>(this.featuresCategories.size());
        FastVector attributes = new FastVector();
        for (IFeature f : this.featuresCategories) {
            Attribute attr = new Attribute(f.toString(), ordinal);
            featureAttribs.put(f, attr);
            attributes.addElement(attr);
        }
        Attribute classAttr = new Attribute("class", classes);

        FastVector methodStrings = new FastVector();
        for (AndroidMethod am : methods)
            methodStrings.addElement(am.getSignature());
        attributes.addElement(classAttr);
        Attribute idAttr = new Attribute("id", methodStrings);
        attributes.addElement(idAttr);

        Instances trainInstances = new Instances("trainingmethodsCat", attributes, 0);
        Instances testInstances = new Instances("allmethodsCat", attributes, 0);
        trainInstances.setClass(classAttr);
        testInstances.setClass(classAttr);

        // Create one instance object per data row
        int instanceId = 0;
        Map<String, AndroidMethod> instanceMethods = new HashMap<String, AndroidMethod>(methods.size());
        Map<Integer, AndroidMethod> instanceIndices = new HashMap<Integer, AndroidMethod>(methods.size());
        for (AndroidMethod am : methods) {
            Instance inst = new Instance(attributes.size());
            inst.setDataset(trainInstances);

            for (Map.Entry<IFeature, Attribute> entry : featureAttribs.entrySet()){
                switch(entry.getKey().applies(am)){
                    case TRUE: inst.setValue(entry.getValue(), "true"); break;
                    case FALSE: inst.setValue(entry.getValue(), "false"); break;
                    default: inst.setMissing(entry.getValue());
                }
            }
            inst.setValue(idAttr, am.getSignature());
            instanceMethods.put(am.getSignature(), am);
            instanceIndices.put(instanceId++, am);

            // Set the known classifications
            if (am.getCategory() == null) {
                inst.setClassMissing();
                testInstances.add(inst);
            }
            else {
                inst.setClassValue(am.getCategory().toString());
                trainInstances.add(inst);
            }
        }
        System.out.println("Running category classifier on "
                + trainInstances.numInstances() + " instances with "
                + attributes.size() + " attributes...");

        ArffSaver saver = new ArffSaver();
        saver.setInstances(trainInstances);
        if (sources)
            saver.setFile(new File("CategoriesSources_Train.arff"));
        else
            saver.setFile(new File("CategoriesSinks_Train.arff"));
        saver.writeBatch();

        try {
//			instances.randomize(new Random(1337));
            int noCatIdx = classes.indexOf("NO_CATEGORY");
            if (noCatIdx < 0)
                throw new RuntimeException("Could not find NO_CATEGORY index");

            Classifier classifier = null;
            if(WEKA_LEARNER_CATEGORIES.equals("BayesNet"))			// (IBK / kNN) vs. SMO vs. (J48 vs. JRIP) vs. NaiveBayes // MultiClassClassifier f黵 ClassifierPerformanceEvaluator
                classifier = new CutoffClassifier(new BayesNet(), THRESHOLD, noCatIdx);
            else if(WEKA_LEARNER_CATEGORIES.equals("NaiveBayes"))
                classifier = new CutoffClassifier(new NaiveBayes(), THRESHOLD, noCatIdx);
            else if(WEKA_LEARNER_CATEGORIES.equals("J48"))
                classifier = new CutoffClassifier(new J48(), THRESHOLD, noCatIdx);
            else if(WEKA_LEARNER_CATEGORIES.equals("SMO"))
//				classifier = new CutoffClassifier(new SMO(), THRESHOLD, noCatIdx);
                classifier = new SMO();
            else if(WEKA_LEARNER_CATEGORIES.equals("JRip"))
                classifier = new CutoffClassifier(new JRip(), THRESHOLD, noCatIdx);
            else
                throw new Exception("Wrong WEKA learner!");

            Evaluation eval = new Evaluation(trainInstances);
			/*for (int foldNum = 0; foldNum < 10; foldNum++) {
				Instances train = trainInstances.trainCV(10, foldNum, new Random(1337));
				Instances test = trainInstances.testCV(10, foldNum);

				Classifier clsCopy = Classifier.makeCopy(classifier);
				clsCopy.buildClassifier(train);

				eval.evaluateModel(clsCopy, test);
			}*/


            StringBuffer sb = new StringBuffer();
            eval.crossValidateModel(classifier, trainInstances,
                    Math.min(trainInstances.numInstances(),10), new Random(1337), sb, new Range(attributes.indexOf(idAttr) + 1 + ""), true);
            System.out.println(sb.toString());

            System.out.println("Class details: " + eval.toClassDetailsString());

            classifier.buildClassifier(trainInstances);
            if(sources)
                weka.core.SerializationHelper.write(modelPath + "/SourceCatModel.model", classifier);
            else
                weka.core.SerializationHelper.write(modelPath + "/SinkCatModel.model", classifier);
            if(WEKA_LEARNER_CATEGORIES.equals("J48")){
                Classifier baseClassifier = ((CutoffClassifier)classifier).getBaseClassifier();
                System.out.println(((J48)(baseClassifier)).graph());
            }
            System.out.println("Record\tSource\tSink\tNN");
            for (int instNum = 0; instNum < testInstances.numInstances(); instNum++) {
                Instance inst = testInstances.instance(instNum);
                assert inst.classIsMissing();
                AndroidMethod meth = instanceMethods.get(inst.stringValue(idAttr));
                double d = classifier.classifyInstance(inst);
                String cName = trainInstances.classAttribute().value((int) d);
                meth.setCategory(AndroidMethod.CATEGORY.valueOf(cName));
            }
        }
        catch (Exception ex) {
            System.err.println("Something went all wonky: " + ex);
            ex.printStackTrace();
        }

        if(DIFF)
            writeCategoryResultsToFiles(targetFileName, methods, sources, sinks, true);
        else
            writeCategoryResultsToFiles(targetFileName, methods, sources, sinks, false);
    }

    /**
     *
     * @param inputFiles txt/csv/pscout
     * @param outputFile pscout
     */
    public void trainSusi(String[] inputFiles, String outputFile) {
        ArrayList<String> inputPara = new ArrayList<>(Arrays.asList(inputFiles));
        inputPara.add(0,jarPath);
        inputPara.add(outputFile);
        String[] args = inputPara.toArray(new String[inputPara.size()]);
        try {
            if (args.length < 3) {
                System.out.println("Usage: java de.ecspride.sourcesinkfinder.SourceSinkFinder "
                        + "<androidJAR> <input1>...<inputN> <outputFile>");
                return;
            }

            String[] inputs = Arrays.copyOfRange(args, 1, args.length-1);

            //set Android paths
            ANDROID = args[0];

            run(inputs, args[args.length - 1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Set<AndroidMethod> PrefilterInterfaces(Set<AndroidMethod> methods) {
        Set<AndroidMethod> purgedMethods = new HashSet<AndroidMethod>(methods.size());
        for (AndroidMethod am : methods) {
            AbstractSootFeature asf = new AbstractSootFeature(ANDROID) {

                @Override
                public Type appliesInternal(AndroidMethod method) {
                    SootMethod sm = SourceSinkParser.getSootMethod(method);
                    if (sm == null)
                        return Type.NOT_SUPPORTED;

                    if (sm.isAbstract() || sm.getDeclaringClass().isInterface()
                            || sm.isPrivate())
                        return Type.FALSE;
                    else
                        return Type.TRUE;
                }
            };

            if (asf.applies(am) == IFeature.Type.TRUE)
                purgedMethods.add(am);
        }
        System.out.println(methods.size() + " methods purged down to " + purgedMethods.size());
        return purgedMethods;
    }

    protected static SootMethod getSootMethod(AndroidMethod method) {
        boolean lookInHierarchy = true;
        SootClass c = Scene.v().forceResolve(method.getClassName(), SootClass.BODIES);
        if (c == null || c.isPhantom()) {
            System.err.println("Class " + method.getClassName() + " not found");
            return null;
        }

        c.setApplicationClass();
        if (c.isInterface())
            return null;

        while (c != null) {
            // Does the current class declare the method we are looking for?
            if (method.getReturnType().isEmpty()) {
                if (c.declaresMethodByName(method.getMethodName()))
                    return c.getMethodByName(method.getMethodName());
            }
            else{
                if (c.declaresMethod(method.getSubSignature()))
                    return c.getMethod(method.getSubSignature());
            }

            // Continue our search up the class hierarchy
            if (lookInHierarchy && c.hasSuperclass())
                c = c.getSuperclass();
            else
                c = null;
        }
        return null;
    }

    @Override
    public void run(String[] inputFiles, String outputFile) throws IOException {
        Set<AndroidMethod> methods = loadMethodsFromFile(inputFiles);

        // Prefilter the interfaces
//        methods = PrefilterInterfaces(methods);

        // Create the custom annotations for derived methods
        createSubclassAnnotations(methods);

        if (LOAD_ANDROID) {
            Set<AndroidMethod> newMethods = new HashSet<AndroidMethod>();
            for (AndroidMethod am : methods)
                if (am.isAnnotated() || am.getCategory() != null)
                    newMethods.add(am);
            methods = newMethods;

            // Load the Android stuff
            loadMethodsFromAndroid(methods);
            createSubclassAnnotations(methods);
        }

        printStatistics(methods);
        methods = sanityCheck(methods);

        // Classify the methods into sources, sinks and neither-nor entries
        startSourceSinkAnalysisTime = System.currentTimeMillis();
        analyzeSourceSinkWeka(methods, outputFile);
        sourceSinkAnalysisTime = System.currentTimeMillis() - startSourceSinkAnalysisTime;
        System.out.println("Time to classify sources/sinks/neither: " + sourceSinkAnalysisTime + " ms");

        // Classify the categories
        if(CLASSIFY_CATEGORY){
            //source
            startCatSourcesTime = System.currentTimeMillis();
            analyzeCategories(methods, outputFile, true, false);
            catSourcesTime = System.currentTimeMillis() - startCatSourcesTime;
            System.out.println("Time to categorize sources: " + catSourcesTime + " ms");

            //sink
            startCatSinksTime = System.currentTimeMillis();
            analyzeCategories(methods, outputFile, false, true);
            catSinksTime = System.currentTimeMillis() - startCatSinksTime;
            System.out.println("Time to categorize sinks: " + catSinksTime + " ms");
        }
        writeRIFLSpecification(outputFile, methods);
    }

    /**
     * read .model and output results
     * @throws Exception
     */
    public void classifyMethods(Set<AndroidMethod> apkMethods, String[] modelPaths,
                                String resultFileName) throws Exception {

        for (String path : modelPaths) {
            if (path.contains("SourceSinkModel")){
                classifySourceSink(apkMethods, path, resultFileName);
            }else if (path.contains("SourceCatModel")){
                classifyCategories(apkMethods, path, resultFileName, true, false);
            }else if (path.contains("SinkCatModel")){
                classifyCategories(apkMethods, path, resultFileName, false, true);
            }else{
                return;
            }
        }
    }

    public void classifySourceSink(Set<AndroidMethod> methods, String modelPath,
                                   String resultFileName) throws Exception {
        FastVector ordinal = new FastVector();
        ordinal.addElement("true");
        ordinal.addElement("false");

        FastVector classes = new FastVector();
        classes.addElement("source");
        classes.addElement("sink");
        classes.addElement("neithernor");

        // Collect all attributes and create the instance set
        Map<IFeature, Attribute> featureAttribs = new HashMap<IFeature, Attribute>(this.featuresSourceSink.size());
        FastVector attributes = new FastVector();
        for (IFeature f : this.featuresSourceSink) {
            Attribute attr = new Attribute(f.toString(), ordinal);
            featureAttribs.put(f, attr);
            attributes.addElement(attr);
        }
        Attribute classAttr = new Attribute("class", classes);

        FastVector methodStrings = new FastVector();
        for (AndroidMethod am : methods)
            methodStrings.addElement(am.getSignature());
        attributes.addElement(classAttr);
        Attribute idAttr = new Attribute("id", methodStrings);
        attributes.addElement(idAttr);

        Instances testInstances = new Instances("allmethods", attributes, 0);
        testInstances.setClass(classAttr);

        // Create one instance object per data row
        int sourceTraining = 0;
        int sinkTraining = 0;
        int nnTraining = 0;
        int instanceId = 0;
        Map<String, AndroidMethod> instanceMethods = new HashMap<String, AndroidMethod>(methods.size());
        Map<Integer, AndroidMethod> instanceIndices = new HashMap<Integer, AndroidMethod>(methods.size());
        for (AndroidMethod am : methods) {
            Instance inst = new Instance(attributes.size());
            inst.setDataset(testInstances);

            for (Map.Entry<IFeature, Attribute> entry : featureAttribs.entrySet()){
                switch(entry.getKey().applies(am)){
                    case TRUE: inst.setValue(entry.getValue(), "true"); break;
                    case FALSE: inst.setValue(entry.getValue(), "false"); break;
                    default: inst.setMissing(entry.getValue());
                }
            }
            inst.setValue(idAttr, am.getSignature());
            instanceMethods.put(am.getSignature(), am);
            instanceIndices.put(instanceId++, am);

            // Set the known classifications
            if (am.isSource()) {
                inst.setClassValue("source");
                sourceTraining++;
            }
            else if (am.isSink()) {
                inst.setClassValue("sink");
                sinkTraining++;
            }
            else if (am.isNeitherNor()) {
                inst.setClassValue("neithernor");
                nnTraining++;
            }
            else
                inst.setClassMissing();

            if (!am.isAnnotated())
                testInstances.add(inst);
        }

        try{
            Classifier classifier = (Classifier) SerializationHelper.read(modelPath);

            if(WEKA_LEARNER_ALL.equals("J48")){
                System.out.println(((J48)(classifier)).graph());
            }
            for (int instIdx = 0; instIdx < testInstances.numInstances(); instIdx++) {
                Instance inst = testInstances.instance(instIdx);
                assert inst.classIsMissing();
                AndroidMethod meth = instanceMethods.get(inst.stringValue(idAttr));
                double d = classifier.classifyInstance(inst);
                String cName = testInstances.classAttribute().value((int) d);
                if (cName.equals("source")) {
                    inst.setClassValue("source");
                    meth.setSource(true);
                }
                else if (cName.equals("sink")) {
                    inst.setClassValue("sink");
                    meth.setSink(true);
                }
                else if (cName.equals("neithernor")) {
                    inst.setClassValue("neithernor");
                    meth.setNeitherNor(true);
                }
                else
                    System.err.println("Unknown class name");
            }
        }
        catch (Exception ex) {
            System.err.println("Something went all wonky: " + ex);
            ex.printStackTrace();
        }

        if(DIFF)
            writeResultsToFiles(resultFileName, methods, true);
        else
            writeResultsToFiles(resultFileName, methods, false);

        Runtime.getRuntime().gc();
    }

    public void classifyCategories(Set<AndroidMethod> methods, String modelPath,
                                   String resultFileName, boolean sources,
                                   boolean sinks) throws IOException {
        FastVector ordinal = new FastVector();
        ordinal.addElement("true");
        ordinal.addElement("false");

//        // We are only interested in sources and sinks
//        {
//            Set<AndroidMethod> newMethods = new HashSet<AndroidMethod>(methods.size());
//            for (AndroidMethod am : methods) {
//                // Make sure that we run after source/sink classification
//                assert am.isAnnotated();
//                if (am.isSink() == sinks && am.isSource() == sources)
//                    newMethods.add(am);
//            }
//            methods = newMethods;
//        }
//        System.out.println("We have a set of " + methods.size() + " sources and sinks.");

        // Build the class attribute, one possibility for every category
        FastVector classes = new FastVector();
        for (AndroidMethod.CATEGORY cat : AndroidMethod.CATEGORY.values()) {
            classes.addElement(cat.toString());
            // Only add the class if it is actually used
//            if (cat == AndroidMethod.CATEGORY.NO_CATEGORY)
//                classes.addElement(cat.toString());
//            else {
//                for (AndroidMethod am : methods)
//                    if (am.isSource() == sources
//                            && am.isSink() == sinks
//                            && am.getCategory() == cat) {
//                        classes.addElement(cat.toString());
//                        break;
//                    }
//            }
        }

        // Collect all attributes and create the instance set
        Map<IFeature, Attribute> featureAttribs = new HashMap<IFeature, Attribute>(this.featuresCategories.size());
        FastVector attributes = new FastVector();
        for (IFeature f : this.featuresCategories) {
            Attribute attr = new Attribute(f.toString(), ordinal);
            featureAttribs.put(f, attr);
            attributes.addElement(attr);
        }
        Attribute classAttr = new Attribute("class", classes);

        FastVector methodStrings = new FastVector();
        for (AndroidMethod am : methods)
            methodStrings.addElement(am.getSignature());
        attributes.addElement(classAttr);
        Attribute idAttr = new Attribute("id", methodStrings);
        attributes.addElement(idAttr);

        Instances testInstances = new Instances("allmethodsCat", attributes, 0);
        testInstances.setClass(classAttr);

        // Create one instance object per data row
        int instanceId = 0;
        Map<String, AndroidMethod> instanceMethods = new HashMap<String, AndroidMethod>(methods.size());
        Map<Integer, AndroidMethod> instanceIndices = new HashMap<Integer, AndroidMethod>(methods.size());
        for (AndroidMethod am : methods) {
            Instance inst = new Instance(attributes.size());
            inst.setDataset(testInstances);

            for (Map.Entry<IFeature, Attribute> entry : featureAttribs.entrySet()){
                switch(entry.getKey().applies(am)){
                    case TRUE: inst.setValue(entry.getValue(), "true"); break;
                    case FALSE: inst.setValue(entry.getValue(), "false"); break;
                    default: inst.setMissing(entry.getValue());
                }
            }
            inst.setValue(idAttr, am.getSignature());
            instanceMethods.put(am.getSignature(), am);
            instanceIndices.put(instanceId++, am);

            // Set the known classifications
            if (am.getCategory() == null) {
                inst.setClassMissing();
                testInstances.add(inst);
            }
            else {
                inst.setClassValue(am.getCategory().toString());
            }
        }


        try {
//			instances.randomize(new Random(1337));
            int noCatIdx = classes.indexOf("NO_CATEGORY");
            if (noCatIdx < 0)
                throw new RuntimeException("Could not find NO_CATEGORY index");

            Classifier classifier = (Classifier) SerializationHelper.read(modelPath);

            if(WEKA_LEARNER_CATEGORIES.equals("J48")){
                Classifier baseClassifier = ((CutoffClassifier)classifier).getBaseClassifier();
                System.out.println(((J48)(baseClassifier)).graph());
            }
//            System.out.println("Record\tSource\tSink\tNN");
            for (int instNum = 0; instNum < testInstances.numInstances(); instNum++) {
                Instance inst = testInstances.instance(instNum);
                assert inst.classIsMissing();
                AndroidMethod meth = instanceMethods.get(inst.stringValue(idAttr));
                double d = classifier.classifyInstance(inst);
                String cName = testInstances.classAttribute().value((int) d);
                meth.setCategory(AndroidMethod.CATEGORY.valueOf(cName));
            }
        }
        catch (Exception ex) {
            System.err.println("Something went all wonky: " + ex);
            ex.printStackTrace();
        }

        if(DIFF)
            writeCategoryResultsToFiles(resultFileName, methods, sources, sinks, true);
        else
            writeCategoryResultsToFiles(resultFileName, methods, sources, sinks, false);
    }


    public void setConfiguration(String modelPath, String jarPath, String resultFile){
        this.modelPath = modelPath;
        this.jarPath = jarPath;
        this.resultFile = resultFile;
    }

    public void apkSourceSinkParser(String apkFile, String[] trainRes) throws Exception {
//        //test feature initialization
//        Set<IFeature> featuresSourceSink = initializeFeaturesSourceSink();
//        Set<IFeature> featuresCategories = initializeFeaturesCategories();

        // parse apk methods
        Set<AndroidMethod> androidMethods = loadApkMethods(jarPath, apkFile, resultFile);

        // classify sources and sinks
        String[] modelPaths = {modelPath + "/SourceSinkModel.model", modelPath + "/SourceCatModel.model",
            modelPath + "/SinkCatModel.model"};

        // Create features for the permissions
        loadMethodsFromFile(trainRes);

        classifyMethods(androidMethods, modelPaths, resultFile);
    }

    public static void appendCategoryToFile(String sinkFile, String sourceFile, String outputFile){
        FileManager.appendTextToFile(sinkFile, outputFile, " -> _SINK_", false);
        FileManager.appendTextToFile(sourceFile, outputFile, " -> _SOURCE_", true);
    }

//    public static void main(String[] args) {
//        appendCategoryToFile("G:\\curriculum\\大四下\\毕设\\code\\TaintBench\\sinks.txt",
//                "G:\\curriculum\\大四下\\毕设\\code\\TaintBench\\sources.txt",
//                "G:\\curriculum\\大四下\\毕设\\code\\TaintBench\\sourcesAndSinks.txt");
//    }

}

