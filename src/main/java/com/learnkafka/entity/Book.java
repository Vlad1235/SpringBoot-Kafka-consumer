package com.learnkafka.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class Book {

  @Id
  private Integer bookId;
  private String bookName;
  private String bookAuthor;
  @OneToOne
  @JoinColumn(name = "libraryEventId") // это как 1 правило нормализации. Не в одной таблице чтобы была инфа, но инфа не может быть логически разделена.
  private LibraryEvent libraryEvent;
}
