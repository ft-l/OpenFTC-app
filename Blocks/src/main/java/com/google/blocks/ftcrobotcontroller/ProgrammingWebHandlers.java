// Copyright 2016 Google Inc.

package com.google.blocks.ftcrobotcontroller;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.blocks.ftcrobotcontroller.util.ClipboardUtil;
import com.google.blocks.ftcrobotcontroller.util.HardwareUtil;
import com.google.blocks.ftcrobotcontroller.util.ProjectsUtil;
import com.google.blocks.ftcrobotcontroller.util.SoundsUtil;
import com.qualcomm.ftccommon.CommandList;
import com.qualcomm.robotcore.eventloop.opmode.FtcRobotControllerServiceState;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.webserver.MimeTypesUtil;
import org.firstinspires.ftc.robotcore.internal.webserver.NoCachingWebHandler;
import org.firstinspires.ftc.robotcore.internal.webserver.RobotControllerWebHandlers;
import org.firstinspires.ftc.robotcore.internal.webserver.SessionParametersGenerator;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandler;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandlerDecorator;
import org.firstinspires.ftc.robotcore.internal.webserver.WebHandlerManager;
import org.firstinspires.ftc.robotcore.internal.webserver.WebServer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

/**
 * A class that provides handlers for all HTTP requests related to programming
 *
 * @author lizlooney@google.com (Liz Looney)
 */
@SuppressWarnings("WeakerAccess")
public class ProgrammingWebHandlers {

  public static final String TAG = ProgrammingWebHandlers.class.getSimpleName();
  @SuppressWarnings("FieldCanBeLocal")
  private static String URI_NAV_BLOCKS_OLD = "/FtcProjects.html";
  @SuppressWarnings("FieldCanBeLocal")
  private static String URI_NAV_BLOCKS = "/FtcBlocksProjects.html";
  @SuppressWarnings("FieldCanBeLocal")
  private static String URI_NAV_ONBOTJAVA = "/java/editor.html";

  private static final String URI_SERVER = "/server";
  private static final String URI_PING = RobotControllerWebHandlers.URI_PING;
  private static final String URI_HARDWARE = "/hardware";
  private static final String URI_TOOLBOX = "/toolbox";
  private static final String URI_LIST_PROJECTS = "/list";
  private static final String URI_LIST_SAMPLES = "/samples";
  private static final String URI_FETCH_BLK = "/fetch_blk";
  private static final String URI_NEW_PROJECT = "/new";
  private static final String URI_SAVE_PROJECT = "/save";
  private static final String URI_RENAME_PROJECT = "/rename";
  private static final String URI_COPY_PROJECT = "/copy";
  private static final String URI_ENABLE_PROJECT = "/enable";
  private static final String URI_DELETE_PROJECTS = "/delete";
  private static final String URI_SAVE_CLIPBOARD = "/savecb";
  private static final String URI_FETCH_CLIPBOARD = "/fetch_cb";
  private static final String URI_LIST_SOUNDS = "/list_sounds";
  private static final String URI_SAVE_SOUND = "/save_sound";
  private static final String URI_FETCH_SOUND = "/fetch_sound";
  private static final String URI_FETCH_SOUND_TYPE = "/fetch_sound_type";
  private static final String URI_RENAME_SOUND = "/rename_sound";
  private static final String URI_COPY_SOUND = "/copy_sound";
  private static final String URI_DELETE_SOUNDS = "/delete_sounds";
  private static final String URI_RESTART_ROBOT = "/restart_robot";
  private static final String URI_COLORS = RobotControllerWebHandlers.URI_COLORS;
  private static final String PARAM_NAME = RobotControllerWebHandlers.PARAM_NAME;
  private static final String PARAM_NEW_NAME = RobotControllerWebHandlers.PARAM_NEW_NAME;
  private static final String PARAM_SAMPLE_NAME = "sample";
  private static final String PARAM_BLK = "blk";
  private static final String PARAM_JS = "js";
  private static final String PARAM_FLAVOR = "flavor";
  private static final String PARAM_GROUP = "group";
  private static final String PARAM_ENABLE = "enable";
  private static final String PARAM_CLIPBOARD = "cb";
  private static final String PARAM_CONTENT = "content";
  private final JavaModeWebHandlers javaModeWebHandlers;

  ///////////////////////////////////////////////////////////////////////////////////////////////
  // WebHandler implementations
  ///////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * Add logging to a WebHandler. This object decorates any RequestHandler that needs to
   * be logged by the ProgrammingModeServer.
   */
  private class LoggingHandler extends WebHandlerDecorator {

    public LoggingHandler(WebHandler delegate) {
      super(delegate);
    }

    @Override
    public  Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      addLogging(session);
      return super.getResponse(session);
    }

    private void addLogging(NanoHTTPD.IHTTPSession session) {
      if (programmingModeLog != null) {
        programmingModeLog.addToLog(session.getUri());
      }
      RobotLog.vv(TAG, "serve uri=%s", session.getUri());
    }
  }

  /**
   * Respond to a ping.
   */
  private class LoggingClientPing extends RobotControllerWebHandlers.ClientPing {

    @Override protected void logPing(NanoHTTPD.IHTTPSession session) {

      if (programmingModeLog != null) {
        programmingModeLog.ping(getPingDetails(session));
      }
    }
  }

  /**
   * Fetch the javaScript for the server.
   */
  private static class Server implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      return fetchJavaScriptForServer(session);
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private Response fetchJavaScriptForServer(NanoHTTPD.IHTTPSession session) throws IOException {
      StringBuilder js = new StringBuilder();
      js.append("var URI_PING = '").append(URI_PING).append("';\n");
      js.append("var URI_HARDWARE = '").append(URI_HARDWARE).append("';\n");
      js.append("var URI_TOOLBOX = '").append(URI_TOOLBOX).append("';\n");
      js.append("var URI_LIST_PROJECTS = '").append(URI_LIST_PROJECTS).append("';\n");
      js.append("var URI_LIST_SAMPLES = '").append(URI_LIST_SAMPLES).append("';\n");
      js.append("var URI_FETCH_BLK = '").append(URI_FETCH_BLK).append("';\n");
      js.append("var URI_NEW_PROJECT = '").append(URI_NEW_PROJECT).append("';\n");
      js.append("var URI_SAVE_PROJECT = '").append(URI_SAVE_PROJECT).append("';\n");
      js.append("var URI_RENAME_PROJECT = '").append(URI_RENAME_PROJECT).append("';\n");
      js.append("var URI_COPY_PROJECT = '").append(URI_COPY_PROJECT).append("';\n");
      js.append("var URI_ENABLE_PROJECT = '").append(URI_ENABLE_PROJECT).append("';\n");
      js.append("var URI_DELETE_PROJECTS = '").append(URI_DELETE_PROJECTS).append("';\n");
      js.append("var URI_SAVE_CLIPBOARD = '").append(URI_SAVE_CLIPBOARD).append("';\n");
      js.append("var URI_FETCH_CLIPBOARD = '").append(URI_FETCH_CLIPBOARD).append("';\n");
      js.append("var URI_LIST_SOUNDS = '").append(URI_LIST_SOUNDS).append("';\n");
      js.append("var URI_SAVE_SOUND = '").append(URI_SAVE_SOUND).append("';\n");
      js.append("var URI_FETCH_SOUND = '").append(URI_FETCH_SOUND).append("';\n");
      js.append("var URI_FETCH_SOUND_TYPE = '").append(URI_FETCH_SOUND_TYPE).append("';\n");
      js.append("var URI_RENAME_SOUND = '").append(URI_RENAME_SOUND).append("';\n");
      js.append("var URI_COPY_SOUND = '").append(URI_COPY_SOUND).append("';\n");
      js.append("var URI_DELETE_SOUNDS = '").append(URI_DELETE_SOUNDS).append("';\n");
      js.append("var URI_RESTART_ROBOT = '").append(URI_RESTART_ROBOT).append("';\n");
      js.append("var PARAM_NAME = '").append(PARAM_NAME).append("';\n");
      js.append("var PARAM_NEW_NAME = '").append(PARAM_NEW_NAME).append("';\n");
      js.append("var PARAM_SAMPLE_NAME = '").append(PARAM_SAMPLE_NAME).append("';\n");
      js.append("var PARAM_BLK = '").append(PARAM_BLK).append("';\n");
      js.append("var PARAM_JS = '").append(PARAM_JS).append("';\n");
      js.append("var PARAM_FLAVOR = '").append(PARAM_FLAVOR).append("';\n");
      js.append("var PARAM_GROUP = '").append(PARAM_GROUP).append("';\n");
      js.append("var PARAM_ENABLE = '").append(PARAM_ENABLE).append("';\n");
      js.append("var PARAM_CLIPBOARD = '").append(PARAM_CLIPBOARD).append("';\n");
      js.append("var PARAM_CONTENT = '").append(PARAM_CONTENT).append("';\n");
      return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, "application/javascript", js.toString()));
    }
  }

  private static class RobotControllerConfiguration extends RobotControllerWebHandlers.RobotControllerConfiguration {
    @Override protected void appendVariables(StringBuilder js) {
      super.appendVariables(js);
      appendVariable(js, "URI_NAV_BLOCKS", URI_NAV_BLOCKS);
      appendVariable(js, "URI_NAV_ONBOTJAVA", URI_NAV_ONBOTJAVA);
    }
  }

  /**
   * Fetches the names of projects.
   */
  private static class ListProjects implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      return fetchProjects(session);
    }

    private Response fetchProjects(NanoHTTPD.IHTTPSession session) throws IOException {
      String jsonProjects = ProjectsUtil.fetchProjectsWithBlocks();
      return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, jsonProjects));
    }
  }

  /**
   * Fetches the names of samples.
   */
  private static class ListSamples implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      return fetchSamples(session);
    }

    private Response fetchSamples(NanoHTTPD.IHTTPSession session) throws IOException {
      String jsonSamples = ProjectsUtil.fetchSamples();
      return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, jsonSamples));
    }
  }

  /**
   * Fetches the JavaScript code related to the hardware in the active configuration.
   */
  private static class Hardware implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      return fetchJavaScriptForHardware(session);
    }

    private Response fetchJavaScriptForHardware(NanoHTTPD.IHTTPSession session) throws IOException {
      String jsHardware = HardwareUtil.fetchJavaScriptForHardware();
      return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, "application/javascript", jsHardware));
    }
  }

  /**
   * Fetches the toolbox xml, with some categories related to the hardware in the active
   * configuration.
   */
  private static class ToolBox implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      return fetchToolbox(session);
    }

    private Response fetchToolbox(NanoHTTPD.IHTTPSession session) throws IOException {
      String xmlToolbox = HardwareUtil.fetchToolbox(AppUtil.getDefContext().getAssets());
      return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, xmlToolbox));
    }
  }

  /**
   * Fetches the content of the blocks file for the given project.
   */
  private static class FetchBlockFile implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
     String name = getFirstNamedParameter(session, PARAM_NAME);
      if (name != null) {
        return fetchBlkFileContent(session, name);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " parameter is required");
      }
    }

    private Response fetchBlkFileContent(NanoHTTPD.IHTTPSession session, String projectName) throws IOException {
      String blkContent = ProjectsUtil.fetchBlkFileContent(projectName);
      return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, blkContent));
    }
  }

  /**
   * Creates a new project.
   */
  private static class NewProject implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String name = getFirstNamedParameter(session, PARAM_NAME);
      if (name != null) {
        String sampleName = getFirstNamedParameter(session, PARAM_SAMPLE_NAME);
        return newProject(session, name, sampleName);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " parameter is required");
      }
    }

    private Response newProject(NanoHTTPD.IHTTPSession session, String projectName, String sampleName) throws IOException {
      String blkContent = ProjectsUtil.newProject(projectName, sampleName);
      return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, blkContent));
    }
  }

  /**
   * Saves the blocks and JavaScript files for the given project.
   */
  private static class SaveProject implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String name = getFirstNamedParameter(session, PARAM_NAME);
      String blk = getFirstNamedParameter(session, PARAM_BLK);
      String js = getFirstNamedParameter(session, PARAM_JS);
      if (name != null && blk != null && js != null) {
        String flavor = getFirstNamedParameter(session, PARAM_FLAVOR);
        String group = getFirstNamedParameter(session, PARAM_GROUP);
        String enable = getFirstNamedParameter(session, PARAM_ENABLE);
        return saveProject(name, blk, js, flavor, group,
            (enable == null) ? true : Boolean.parseBoolean(enable));
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + ", " + PARAM_BLK + ", and " + PARAM_JS + " parameters are required");
      }
    }

    private Response saveProject(String projectName, String blkContent, String jsFileContent,
        @Nullable String flavor, @Nullable String group, boolean enable) throws IOException {
      ProjectsUtil.saveProject(projectName, blkContent, jsFileContent, flavor, group, enable);
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }

  /**
   * Renames the given project.
   */
  private static class RenameProject implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String oldName = getFirstNamedParameter(session, PARAM_NAME);
      String newName = getFirstNamedParameter(session, PARAM_NEW_NAME);
      if (oldName != null && newName != null) {
        return renameProject(oldName, newName);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " and " + PARAM_NEW_NAME + " parameters are required");
      }
    }

    private Response renameProject(String oldProjectName, String newProjectName)
            throws IOException {
      ProjectsUtil.renameProject(oldProjectName, newProjectName);
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }

  /**
   * Copies the given project.
   */
  private static class CopyProject implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String oldName = getFirstNamedParameter(session, PARAM_NAME);
      String newName = getFirstNamedParameter(session, PARAM_NEW_NAME);
      if (oldName != null && newName != null) {
        return copyProject(oldName, newName);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " and " + PARAM_NEW_NAME + " parameters are required");
      }
    }

    private Response copyProject(String oldProjectName, String newProjectName)
            throws IOException {
      ProjectsUtil.copyProject(oldProjectName, newProjectName);
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }

  /**
   * Enables (or disables) the given project.
   */
  private static class EnableProject implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String name = getFirstNamedParameter(session, PARAM_NAME);
      String enable = getFirstNamedParameter(session, PARAM_ENABLE);
      if (name != null && enable != null) {
        return enableProject(name, Boolean.parseBoolean(enable));
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " and " + PARAM_NEW_NAME + " parameters are required");
      }
    }

    private Response enableProject(String projectName, boolean enable)
            throws IOException {
      ProjectsUtil.enableProject(projectName, enable);
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }

  /**
   * Deletes the projects with the given names.
   */
  private static class DeleteProjects implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String names = getFirstNamedParameter(session, PARAM_NAME);
      if (names != null) {
        return deleteProjects(names);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " parameter is required");
      }
    }

    private Response deleteProjects(String starDelimitedProjectNames) throws IOException {
      String[] projectNames = starDelimitedProjectNames.split("\\*");
      ProjectsUtil.deleteProjects(projectNames);
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }

  /**
   * Saves the clipboard content.
   */
  private static class SaveClipboard implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String clipboardContent = getFirstNamedParameter(session, PARAM_CLIPBOARD);
      if (clipboardContent != null) {
        return saveClipboardContent(clipboardContent);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_CLIPBOARD + " parameter is required");
      }
    }

    private Response saveClipboardContent(String clipboardContent) throws IOException {
      ClipboardUtil.saveClipboardContent(clipboardContent);
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }

  /**
   * Fetches the clipboard content.
   */
  private static class FetchClipboard implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      return fetchClipboardContent();
    }

    private Response fetchClipboardContent() throws IOException {
      String clipboardContent = ClipboardUtil.fetchClipboardContent();
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, clipboardContent);
    }
  }

  /**
   * Fetches the names of sounds.
   */
  private static class ListSounds implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      return fetchSounds(session);
    }

    private Response fetchSounds(NanoHTTPD.IHTTPSession session) throws IOException {
      String jsonSounds = SoundsUtil.fetchSounds();
      return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, jsonSounds));
    }
  }

  /**
   * Saves a sound file.
   */
  private static class SaveSound implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String name = getFirstNamedParameter(session, PARAM_NAME);
      String base64Content = getFirstNamedParameter(session, PARAM_CONTENT);
      if (name != null && base64Content != null) {
        return saveSoundFile(name, base64Content);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " and " + PARAM_CONTENT + " parameters are required");
      }
    }

    private Response saveSoundFile(String soundName, String base64Content) throws IOException {
      SoundsUtil.saveSoundFile(soundName, base64Content);
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }


  /**
   * Fetches the content of the sound file with the given sound name.
   */
  private static class FetchSound implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String name = getFirstNamedParameter(session, PARAM_NAME);
      if (name != null) {
        return fetchSoundFileContent(session, name);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " parameter is required");
      }
    }

    private Response fetchSoundFileContent(NanoHTTPD.IHTTPSession session, String soundName) throws IOException {
      String base64Content = SoundsUtil.fetchSoundFileContent(soundName);
      return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, base64Content));
    }
  }

  /**
   * Fetches the mime type of the sound file with the given sound name.
   */
  private static class FetchSoundType implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String name = getFirstNamedParameter(session, PARAM_NAME);
        if (name != null) {
          return fetchSoundFileMimeType(session, name);
        } else {
          return newFixedLengthResponse(
                  Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                  "Bad Request: " + PARAM_NAME + " parameter is required");
        }
    }

    private Response fetchSoundFileMimeType(NanoHTTPD.IHTTPSession session, String soundName) throws IOException {
      String mimeType = MimeTypesUtil.determineMimeType(soundName);
      if (mimeType == null) {
        mimeType = "";
      }
      return NoCachingWebHandler.setNoCache(session, newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, mimeType));
    }
  }


  /**
   * Renames the given sound.
   */
  private static class RenameSound implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String oldName = getFirstNamedParameter(session, PARAM_NAME);
      String newName = getFirstNamedParameter(session, PARAM_NEW_NAME);
      if (oldName != null && newName != null) {
        return renameSound(oldName, newName);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " and " + PARAM_NEW_NAME + " parameters are required");
      }
    }

    private Response renameSound(String oldSoundName, String newSoundName)
            throws IOException {
      SoundsUtil.renameSound(oldSoundName, newSoundName);
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }


  /**
   * Copies the given sound.
   */
  private static class CopySound implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String oldName = getFirstNamedParameter(session, PARAM_NAME);
      String newName = getFirstNamedParameter(session, PARAM_NEW_NAME);
      if (oldName != null && newName != null) {
        return copySound(oldName, newName);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " and " + PARAM_NEW_NAME + " parameters are required");
      }
    }

    private Response copySound(String oldSoundName, String newSoundName)
            throws IOException {
      SoundsUtil.copySound(oldSoundName, newSoundName);
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }

  /**
   * Deletes the sounds with the given names.
   */
  private static class DeleteSounds implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      String names = getFirstNamedParameter(session, PARAM_NAME);
      if (names != null) {
        return deleteSounds(names);
      } else {
        return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                "Bad Request: " + PARAM_NAME + " parameter is required");
      }
    }

    private Response deleteSounds(String starDelimitedSoundNames) throws IOException {
      String[] soundNames = starDelimitedSoundNames.split("\\*");
      SoundsUtil.deleteSounds(soundNames);
      return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "");
    }
  }

  /**
   * RestartRobot
   */
  private static class RestartRobot implements WebHandler {

    @Override
    public Response getResponse(NanoHTTPD.IHTTPSession session) throws IOException, NanoHTTPD.ResponseException {
      final NetworkConnectionHandler connectionHandler = NetworkConnectionHandler.getInstance();
      connectionHandler.injectReceivedCommand(new Command(CommandList.CMD_RESTART_ROBOT));
      return WebHandlerManager.OK_RESPONSE;
    }
  }


///////////////////////////////////////////////////////////////////////////////////////////////
  // ProgrammingWebHandlers
  ///////////////////////////////////////////////////////////////////////////////////////////////

  private volatile ProgrammingModeLog programmingModeLog;
  private WebServer webServer;

  /**
   * Constructs a {@link ProgrammingWebHandlers} with the given port.
   */
  public ProgrammingWebHandlers() {
    javaModeWebHandlers = new JavaModeWebHandlers();
  }

  public void setState(FtcRobotControllerServiceState state) {
    this.webServer = state.getWebServer();
    registerHandlers(webServer.getWebHandlerManager());

    javaModeWebHandlers.setState(state);
  }

  private void registerHandlers(WebHandlerManager manager) {

    manager.register(URI_NAV_BLOCKS_OLD,   new RobotControllerWebHandlers.Redirection("/"));
    manager.register(URI_PING,             decorateWithParms(new LoggingClientPing()));
    manager.register(URI_SERVER,           decorateWithLogging(new Server()));
    manager.register(URI_HARDWARE,         decorateWithLogging(new Hardware()));
    manager.register(URI_TOOLBOX,          decorateWithLogging(new ToolBox()));
    manager.register(URI_LIST_PROJECTS,    decorateWithLogging(new ListProjects()));
    manager.register(URI_LIST_SAMPLES,     decorateWithLogging(new ListSamples()));
    manager.register(URI_FETCH_BLK,        decorateWithLogging(decorateWithParms(new FetchBlockFile())));
    manager.register(URI_NEW_PROJECT,      decorateWithLogging(decorateWithParms(new NewProject())));
    manager.register(URI_SAVE_PROJECT,     decorateWithLogging(decorateWithParms(new SaveProject())));
    manager.register(URI_RENAME_PROJECT,   decorateWithLogging(decorateWithParms(new RenameProject())));
    manager.register(URI_COPY_PROJECT,     decorateWithLogging(decorateWithParms(new CopyProject())));
    manager.register(URI_ENABLE_PROJECT,   decorateWithLogging(decorateWithParms(new EnableProject())));
    manager.register(URI_DELETE_PROJECTS,  decorateWithLogging(decorateWithParms(new DeleteProjects())));
    manager.register(URI_SAVE_CLIPBOARD,   decorateWithLogging(decorateWithParms(new SaveClipboard())));
    manager.register(URI_FETCH_CLIPBOARD,  decorateWithLogging(new FetchClipboard()));
    manager.register(URI_LIST_SOUNDS,      decorateWithLogging(new ListSounds()));
    manager.register(URI_SAVE_SOUND,       decorateWithLogging(decorateWithParms(new SaveSound())));
    manager.register(URI_FETCH_SOUND,      decorateWithLogging(decorateWithParms(new FetchSound())));
    manager.register(URI_FETCH_SOUND_TYPE, decorateWithLogging(decorateWithParms(new FetchSoundType())));
    manager.register(URI_RENAME_SOUND,     decorateWithLogging(decorateWithParms(new RenameSound())));
    manager.register(URI_COPY_SOUND,       decorateWithLogging(decorateWithParms(new CopySound())));
    manager.register(URI_DELETE_SOUNDS,    decorateWithLogging(decorateWithParms(new DeleteSounds())));
    manager.register(URI_RESTART_ROBOT,    decorateWithLogging(new RestartRobot()));
    manager.register(URI_COLORS,           decorateWithLogging(manager.getRegistered(URI_COLORS)));

    javaModeWebHandlers.register(manager, new ProgrammingModeWebHandlerDecorator());
    manager.register(RobotControllerWebHandlers.URI_RC_CONFIG,  new RobotControllerConfiguration());
  }

  class ProgrammingModeWebHandlerDecorator {
    <T extends WebHandler> WebHandler decorate(T handler, boolean decorateWithParams) {
      WebHandler wh = decorateWithParams ? decorateWithParms(handler) : handler;
      return decorateWithLogging(wh);
    }
  }

  static String getFirstNamedParameter(NanoHTTPD.IHTTPSession session, String name) {
    final Map<String, List<String>> parameters = session.getParameters();
    if (!parameters.containsKey(name)) return null;
    return parameters.get(name).get(0);
  }

  /**
   * add logging to a {@link WebHandler}
   */
  private WebHandler decorateWithLogging(WebHandler delegate) {
    if (delegate instanceof LoggingHandler) {
      return delegate;
    } else {
      return new LoggingHandler(delegate);
    }
  }

  /**
   * add parms generation to a {@link WebHandler}
   */
  private WebHandler decorateWithParms(WebHandler delegate) {
    return new SessionParametersGenerator(delegate);
  }

  /**
   * Set the programmingModeLog
   * @param programmingModeLog - logging for programmingMode.
   */
  void setProgrammingModeLog(@NonNull ProgrammingModeLog programmingModeLog) {
    this.programmingModeLog = programmingModeLog;
  }

  public WebServer getWebServer() {
    return webServer;
  }
}
