create table person (
    id int not null auto_increment,
    first_name varchar(255) not null,
    date_of_birth date not null,
    added_at timestamp not null default current_timestamp,
    primary key (id)
);
