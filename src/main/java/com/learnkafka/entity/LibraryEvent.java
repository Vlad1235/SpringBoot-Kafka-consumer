package com.learnkafka.entity;


import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
Это информация, которую будет производить producer
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class LibraryEvent {

  @Id
  @GeneratedValue
  private Integer libraryEventId;
  @Enumerated(EnumType.STRING)
  private LibraryEventType libraryEventType;
  @OneToOne(mappedBy = "libraryEvent",cascade = {CascadeType.ALL})
  @ToString.Exclude // avoid circular reference(Book also has reference to LibraryEvent) and therefore stackoverflow
  private Book book;

}
