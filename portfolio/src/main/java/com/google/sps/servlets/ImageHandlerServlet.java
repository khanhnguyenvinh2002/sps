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
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
/**
 * Takes an image submitted by the user and uploads it to Cloud Storage, and then displays it as
 * HTML in the response.
 */
@WebServlet("/upload")
@MultipartConfig
public class ImageHandlerServlet extends HttpServlet {

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    // Get the message entered by the user.
    String message = request.getParameter("message");

    // Get the file chosen by the user.
    Part filePart = request.getPart("image");
    String fileName = System.currentTimeMillis() + filePart.getSubmittedFileName();
    InputStream fileInputStream = filePart.getInputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // Code simulating the copy
    // You could alternatively use NIO
    // And please, unlike me, do something about the Exceptions :D
    byte[] buffer = new byte[1024];
    int len;
    while ((len = fileInputStream.read(buffer)) > -1 ) {
        baos.write(buffer, 0, len);
    }
    baos.flush();
        
    // Open new InputStreams using recorded bytes
    // Can be repeated as many times as you wish
    InputStream is1 = new ByteArrayInputStream(baos.toByteArray()); 
    InputStream is2 = new ByteArrayInputStream(baos.toByteArray()); 
    // Get the labels of the image that the user uploaded.
    List<EntityAnnotation> imageLabels = getImageLabels(is1.readAllBytes());

    ArrayList<Label> labelItems = new ArrayList<Label>();
    for(EntityAnnotation label : imageLabels){
        labelItems.add(new Label(label.getDescription(), label.getScore()));
    }
    String labels = convertToJsonUsingGson(labelItems);
    String uploadedFileUrl = uploadToCloudStorage(fileName, is2,message, labels);
    // Output some HTML that shows the data the user entered.
    // You could also store the uploadedFileUrl in Datastore instead.
    PrintWriter out = response.getWriter();
    out.println("<p>Here's the image you uploaded:</p>");
    out.println("<a href=\"" + uploadedFileUrl + "\">");
    out.println("<img src=\"" + uploadedFileUrl + "\" />");
    out.println("</a>");
    out.println("<p>Here's the text you entered:</p>");
    out.println(message);
    out.println("<p>Here are the labels we extracted:</p>");
    out.println("<ul>");
    for (EntityAnnotation label : imageLabels) {
      out.println("<li>" + label.getDescription() + " " + label.getScore());
    }
    out.println("</ul>");
  }

  /** Uploads a file to Cloud Storage and returns the uploaded file's URL. */
  private static String uploadToCloudStorage(String imageName, InputStream fileInputStream, String message, String labels) {
    String projectId = "knguyen-sps-summer22";
    String bucketName = "knguyen-sps-summer22.appspot.com";
    Storage storage =
        StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    BlobId blobId = BlobId.of(bucketName, imageName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

    // Upload the file to Cloud Storage.
    Blob blob = storage.create(blobInfo, fileInputStream);

    long timestamp = System.currentTimeMillis();

    Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    KeyFactory keyFactory = datastore.newKeyFactory().setKind("imageName");
    FullEntity imageNameEntity =
        Entity.newBuilder(keyFactory.newKey())
            .set("imageName", imageName)
            .set("message", message)
            .set("labels", labels)
            .set("url", blob.getMediaLink())
            .set("timestamp", timestamp)
            .build();
    datastore.put(imageNameEntity);
    // Return the uploaded file's URL.
    return blob.getMediaLink();
  }

  /**
   * Uses the Google Cloud Vision API to generate a list of labels that apply to the image
   * represented by the binary data stored in imgBytes.
   */
  private List<EntityAnnotation> getImageLabels(byte[] imageBytes) throws IOException {
    ByteString byteString = ByteString.copyFrom(imageBytes);
    Image image = Image.newBuilder().setContent(byteString).build();

    Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
    AnnotateImageRequest request =
        AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build();
    List<AnnotateImageRequest> requests = new ArrayList<>();
    requests.add(request);

    ImageAnnotatorClient client = ImageAnnotatorClient.create();
    BatchAnnotateImagesResponse batchResponse = client.batchAnnotateImages(requests);
    client.close();
    List<AnnotateImageResponse> imageResponses = batchResponse.getResponsesList();
    AnnotateImageResponse imageResponse = imageResponses.get(0);

    if (imageResponse.hasError()) {
      System.err.println("Error getting image labels: " + imageResponse.getError().getMessage());
      return null;
    }

    return imageResponse.getLabelAnnotationsList();
  }

  /**
   * Converts a ServerStats instance into a JSON string using the Gson library. Note: We first added
   * the Gson library dependency to pom.xml.
   */
  private String convertToJsonUsingGson(List<Label> array) {
    Gson gson = new Gson();
    String json = gson.toJson(array);
    return json;
  }
}
