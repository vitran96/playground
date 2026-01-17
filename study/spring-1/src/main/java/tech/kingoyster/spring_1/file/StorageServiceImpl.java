package tech.kingoyster.spring_1.file;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {
    private final FileProperties fileProperties;

    @Override
    public byte[] getFile(String filename) {
        return null;
    }

    @Override
    public List<String> getAll() {
        return null;
    }

    @Override
    public void saveFile(String filename, byte[] content) {
        //
    }
}
