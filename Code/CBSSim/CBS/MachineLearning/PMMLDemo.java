package org.cloudbus.cloudsim.examples.CBS.MachineLearning;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.*;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PMMLDemo {
      public String model_dir = "E://pycharm project/New_Bin_PackingTest/riops_PMMLPredictor_Shanghai"; //模型路径
      private Evaluator loadPmml(){
            PMML pmml = new PMML();
            InputStream inputStream = null;
            try {
                  inputStream = new FileInputStream(this.model_dir);
            } catch (IOException e) {
                  e.printStackTrace();
            }
            if(inputStream == null){
                  return null;
            }
            InputStream is = inputStream;
            try {
                  pmml = org.jpmml.model.PMMLUtil.unmarshal(is);
            } catch (SAXException e1) {
                  e1.printStackTrace();
            } catch (JAXBException e1) {
                  e1.printStackTrace();
            }finally {
                  //关闭输入流
                  try {
                        is.close();
                  } catch (IOException e) {
                        e.printStackTrace();
                  }
            }
            ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
            Evaluator evaluator = modelEvaluatorFactory.newModelEvaluator(pmml);
            pmml = null;
            return evaluator;
      }
      private int predict(Evaluator evaluator,long[] input) {
            Map<String, Long> data = new HashMap<String, Long>();
            for(int i=0;i<input.length;i++){
                  data.put("x"+(i+1),input[i]);
            }
//            data.put("x1", a);
//            data.put("x2", b);
//            data.put("x3", c);
//            data.put("x4", d);
            List<InputField> inputFields = evaluator.getInputFields();
            //过模型的原始特征，从画像中获取数据，作为模型输入
            Map<FieldName, FieldValue> arguments = new LinkedHashMap<FieldName, FieldValue>();
            for (InputField inputField : inputFields) {
                  FieldName inputFieldName = inputField.getName();
                  Object rawValue = data.get(inputFieldName.getValue());
                  FieldValue inputFieldValue = inputField.prepare(rawValue);
                  arguments.put(inputFieldName, inputFieldValue);
            }

            Map<FieldName, ?> results = evaluator.evaluate(arguments);
            List<TargetField> targetFields = evaluator.getTargetFields();

            TargetField targetField = targetFields.get(0);
            FieldName targetFieldName = targetField.getName();

            Object targetFieldValue = results.get(targetFieldName);
            System.out.println("target: " + targetFieldName.getValue() + " value: " + targetFieldValue);
            int primitiveValue = -1;
            if (targetFieldValue instanceof Computable) {
                  Computable computable = (Computable) targetFieldValue;
                  primitiveValue = (Integer)computable.getResult();
            }
            System.out.println("预测结果为:" + targetFieldValue);
            return primitiveValue;
      }
      public static void main(String args[]){
            PMMLDemo demo = new PMMLDemo();
            Evaluator model = demo.loadPmml();
            long[] test = {31622410,1301008980, 0, 1, 1, 0, 1, 0, -5989016486017671160L, 1, 2, 50}; // input长度为特征数量12个，否则无法预测
            demo.predict(model,test);

      }
}