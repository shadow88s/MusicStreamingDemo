package server.dispatcher;

/**
 * The Dispatcher implements DispatcherInterface.
 *
 * @author  Oscar Morales-Ponce
 * @version 0.15
 * @since   02-11-2019
 */

import java.util.HashMap;
import java.util.*;
import java.lang.reflect.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


public class Dispatcher implements DispatcherInterface
{
    HashMap<String, Object> ListOfObjects;
    HashMap<String, String> history;


    public Dispatcher()
    {
        ListOfObjects = new HashMap<>();
        history = new HashMap<>();
    }

    /*
    * dispatch: Executes the remote method in the corresponding Object
    * @param request: Request: it is a Json file
    {
        "remoteMethod":"getSongChunk",
        "objectName":"SongServices",
        "param":
          {
              "song":490183,
              "fragment":2
          }
    }
    */
    public String dispatch(String request)
    {
        JsonObject jsonReturn = new JsonObject();
        JsonParser parser = new JsonParser();
//        System.out.println("Dispatcher Request = " + request.trim());
        JsonObject jsonRequest = parser.parse(request.trim()).getAsJsonObject();
        String id = jsonRequest.get("id").getAsString();
        String semantic = jsonRequest.get("semantic").getAsString();
        if(semantic.equals("at most one") && history.containsKey(id))
        {
            System.out.println("Duplicate Request");
            return history.get(id);
        }

        try {
            // Obtains the object pointing to SongServices
            Object object = ListOfObjects.get(jsonRequest.get("objectName").getAsString());
            Method[] methods = object.getClass().getMethods();
            Method method = null;
            // Obtains the method
            for (int i=0; i<methods.length; i++)
            {
                if (methods[i].getName().equals(jsonRequest.get("remoteMethod").getAsString()))
                {
//                    System.out.println("Method found");
                    method = methods[i];
                }
            }
            if (method == null)
            {
                jsonReturn.addProperty("error", "Method does not exist");
                return jsonReturn.toString();
            }
            // Prepare the  parameters
            Class[] types =  method.getParameterTypes();
            Object[] parameter = new Object[types.length];
            String[] strParam = new String[types.length];
            JsonObject jsonParam = jsonRequest.get("param").getAsJsonObject();
            int j = 0;
            for (Map.Entry<String, JsonElement>  entry  :  jsonParam.entrySet())
            {
                strParam[j++] = entry.getValue().getAsString();
//                System.out.println("Method Param = " + strParam[0]);
            }
            // Prepare parameters
            for (int i=0; i<types.length; i++)
            {
                switch (types[i].getCanonicalName())
                {
                    case "java.lang.Long":
                        parameter[i] =  Long.parseLong(strParam[i]);
                        break;
                    case "java.lang.Integer":
                        parameter[i] =  Integer.parseInt(strParam[i]);
                        break;
                    case "java.lang.String":
                        parameter[i] = strParam[i];
                        break;
                }
            }
            // Prepare the return
            Class returnType = method.getReturnType();
            String ret = "";
            switch (returnType.getCanonicalName())
            {
                case "java.lang.Long":
                    ret = method.invoke(object, parameter).toString();
                    break;
                case "java.lang.Integer":
                    ret = method.invoke(object, parameter).toString();
                    break;
                case "java.lang.String":
                    ret = (String)method.invoke(object, parameter);
                    break;
            }
            jsonReturn.addProperty("ret", ret);

        } catch (InvocationTargetException | IllegalAccessException e)
        {
            //    System.out.println(e);
            jsonReturn.addProperty("error", "Error on " + jsonRequest.get("objectName").getAsString() + "." + jsonRequest.get("remoteMethod").getAsString());
        }

//        System.out.println("Dispatcher Return = " + jsonReturn.toString());
        history.put(id, jsonReturn.toString());
        return jsonReturn.toString();
    }

    /*
     * registerObject: It register the objects that handle the request
     * @param remoteMethod: It is the name of the method that
     *  objectName implements.
     * @objectName: It is the main class that contaions the remote methods
     * each object can contain several remote methods
     */
    public void registerObject(Object remoteMethod, String objectName)
    {
        ListOfObjects.put(objectName, remoteMethod);
//        System.out.println("Objects size = " + ListOfObjects.size());
    }

}
