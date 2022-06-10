package com.google.sps.servlets;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

@WebServlet("/random-fact")
public final class ArrayServlet extends HttpServlet {

  private ArrayList<String> arrayMessage = new ArrayList<String>();


  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Calculate server stats
    arrayMessage.add("I can code");
    arrayMessage.add("I can swim");
    arrayMessage.add("I like cats");
    arrayMessage.add("I can play guitar");
    
    
    String json = convertToJsonUsingGson(arrayMessage);

    // Send the JSON as the response
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  /**
   * Converts a ServerStats instance into a JSON string using the Gson library. Note: We first added
   * the Gson library dependency to pom.xml.
   */
  private String convertToJsonUsingGson(ArrayList<String> array) {
    Gson gson = new Gson();
    String json = gson.toJson(array);
    return json;
  }
}
