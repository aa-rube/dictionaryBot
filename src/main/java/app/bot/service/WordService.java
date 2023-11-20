package app.bot.service;

import app.bot.repository.WordRepository;
import app.bot.model.Word;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WordService {
    @Autowired
    private WordRepository wordRepository;

    public void save(String word) {
        wordRepository.save(new Word(word));
    }

    public List<Word> findAllWords() {
        List<Word> words = wordRepository.findAll();
        if (words.isEmpty()) {
            Word word = new Word("test mode");
            words.add(word);
        }
        return words;
    }

    public String findById(int id) {
        return wordRepository.findById(id).get().getWord();
    }

    public void deleteById(int id) {
        wordRepository.deleteById(id);
    }

}
