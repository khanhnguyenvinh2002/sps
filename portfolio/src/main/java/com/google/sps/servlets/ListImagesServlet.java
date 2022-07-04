// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet responsible for listing tasks. */
@WebServlet("/image-tasks")
public class ListImagesServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    Query<Entity> query =
        Query.newEntityQueryBuilder().setKind("imageName").setOrderBy(OrderBy.desc("timestamp")).build();
    QueryResults<Entity> results = datastore.run(query);

    response.setContentType("text/html;");
    while (results.hasNext()) {
        Entity entity = results.next();
        String message = entity.getString("message");
        String url = entity.getString("url");
        String labels = entity.getString("labels");
        Gson gson = new Gson();
        Label[] data =  gson.fromJson(labels, Label[].class);

        String imgTag = String.format("<img src=\"%s\" />", url);
        String name = String.format("<h2>%s<h2/>", message);
        response.getWriter().println(imgTag);
        response.getWriter().println(name);
        response.getWriter().println("<p>Here are the labels we extracted:</p>");
        response.getWriter().println("<ul>");
        for (Label label : data) {
            response.getWriter().println("<li>" + label.description + " " + label.score);
        }
        response.getWriter().println("</ul>");
        response.getWriter().println("<br><br>");
    }
  }
}

class Image {
    String url;
    String imageName;
    long timestamp;
    long id;
    public Image(long id, String url, String imageName, long timestamp){
        this.url = url;
        this.imageName = imageName;
        this.timestamp = timestamp;
        this.id = id;
    }
}
class Label {
    String description;
    float score;
    public Label(String description, float score){
        this.score = score;
        this.description = description;
    }
}