package es.bsc.compss.types.request.ap;

import java.io.File;

import es.bsc.compss.components.impl.AccessProcessor;
import es.bsc.compss.components.impl.DataInfoProvider;
import es.bsc.compss.components.impl.TaskAnalyser;
import es.bsc.compss.components.impl.TaskDispatcher;
import es.bsc.compss.types.data.FileInfo;
import es.bsc.compss.types.data.location.DataLocation;

public class DeleteFileRequest extends APRequest {

    private final DataLocation loc;

    public DeleteFileRequest(DataLocation loc) {
        this.loc = loc;
    }

    public DataLocation getLocation() {
        return loc;
    }

    @Override
    public void process(AccessProcessor ap, TaskAnalyser ta, DataInfoProvider dip, TaskDispatcher td) {
        FileInfo fileInfo = dip.deleteData(loc);

        if (fileInfo == null) {
            // File is not used by any task
            File f = new File(loc.getPath());
            if (f.delete()) {
                LOGGER.info("File " + loc.getPath() + "deleted");
            } else {
                LOGGER.error("Error on deleting file " + loc.getPath());
            }

        } else { // file is involved in some task execution
            // File Won't be read by any future task or from the main code.
            // Remove it from the dependency analysis and the files to be transferred back
            ta.deleteFile(fileInfo);
        }
    }

    @Override
    public APRequestType getRequestType() {
        return APRequestType.DELETE_FILE;
    }

}