package com.itantra.cache;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CachedPhraseDao_Impl implements CachedPhraseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CachedPhrase> __insertionAdapterOfCachedPhrase;

  private final SharedSQLiteStatement __preparedStmtOfIncrementHitCount;

  public CachedPhraseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCachedPhrase = new EntityInsertionAdapter<CachedPhrase>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `cached_phrases` (`id`,`label`,`embedding`,`audio_path`,`lang_code`,`hit_count`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CachedPhrase entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getLabel() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getLabel());
        }
        statement.bindBlob(3, entity.getEmbedding());
        statement.bindString(4, entity.getAudioPath());
        statement.bindString(5, entity.getLangCode());
        statement.bindLong(6, entity.getHitCount());
      }
    };
    this.__preparedStmtOfIncrementHitCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE cached_phrases SET hit_count = hit_count + 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPhrase(final CachedPhrase phrase,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCachedPhrase.insert(phrase);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementHitCount(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementHitCount.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementHitCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getPhrasesForLang(final String langCode,
      final Continuation<? super List<CachedPhrase>> $completion) {
    final String _sql = "SELECT * FROM cached_phrases WHERE lang_code = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, langCode);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CachedPhrase>>() {
      @Override
      @NonNull
      public List<CachedPhrase> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfEmbedding = CursorUtil.getColumnIndexOrThrow(_cursor, "embedding");
          final int _cursorIndexOfAudioPath = CursorUtil.getColumnIndexOrThrow(_cursor, "audio_path");
          final int _cursorIndexOfLangCode = CursorUtil.getColumnIndexOrThrow(_cursor, "lang_code");
          final int _cursorIndexOfHitCount = CursorUtil.getColumnIndexOrThrow(_cursor, "hit_count");
          final List<CachedPhrase> _result = new ArrayList<CachedPhrase>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CachedPhrase _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpLabel;
            if (_cursor.isNull(_cursorIndexOfLabel)) {
              _tmpLabel = null;
            } else {
              _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            }
            final byte[] _tmpEmbedding;
            _tmpEmbedding = _cursor.getBlob(_cursorIndexOfEmbedding);
            final String _tmpAudioPath;
            _tmpAudioPath = _cursor.getString(_cursorIndexOfAudioPath);
            final String _tmpLangCode;
            _tmpLangCode = _cursor.getString(_cursorIndexOfLangCode);
            final int _tmpHitCount;
            _tmpHitCount = _cursor.getInt(_cursorIndexOfHitCount);
            _item = new CachedPhrase(_tmpId,_tmpLabel,_tmpEmbedding,_tmpAudioPath,_tmpLangCode,_tmpHitCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
