#[derive(Debug, Clone, PartialEq)]
pub enum SExpr {
    Symbol(String),
    String(String),
    Char(char),
    Int(i64),
    List(Vec<SExpr>),
}

impl SExpr {
    pub fn as_symbol(&self) -> Option<&str> {
        match self {
            SExpr::Symbol(s) => Some(s.as_str()),
            _ => None,
        }
    }

    pub fn as_string(&self) -> Option<&str> {
        match self {
            SExpr::String(s) => Some(s.as_str()),
            _ => None,
        }
    }

    pub fn as_int(&self) -> Option<i64> {
        match self {
            SExpr::Int(i) => Some(*i),
            _ => None,
        }
    }

    pub fn as_list(&self) -> Option<&[SExpr]> {
        match self {
            SExpr::List(list) => Some(list.as_slice()),
            _ => None,
        }
    }
}

pub struct SExprParser<'a> {
    chars: Vec<char>,
    pos: usize,
    _marker: std::marker::PhantomData<&'a ()>,
}

impl<'a> SExprParser<'a> {
    pub fn new(input: &str) -> Self {
        Self {
            chars: input.chars().collect(),
            pos: 0,
            _marker: std::marker::PhantomData,
        }
    }

    fn skip_whitespace_and_comments(&mut self) {
        while self.pos < self.chars.len() {
            let c = self.chars[self.pos];
            if c.is_whitespace() {
                self.pos += 1;
            } else if c == ';' {
                // Comment until end of line
                self.pos += 1;
                while self.pos < self.chars.len() && self.chars[self.pos] != '\n' {
                    self.pos += 1;
                }
            } else {
                break;
            }
        }
    }

    pub fn parse_all(&mut self) -> Result<Vec<SExpr>, String> {
        let mut exprs = Vec::new();
        loop {
            self.skip_whitespace_and_comments();
            if self.pos >= self.chars.len() {
                break;
            }
            exprs.push(self.parse_sexpr()?);
        }
        Ok(exprs)
    }

    pub fn parse_sexpr(&mut self) -> Result<SExpr, String> {
        self.skip_whitespace_and_comments();
        if self.pos >= self.chars.len() {
            return Err("Unexpected EOF while parsing S-expression".to_string());
        }

        let c = self.chars[self.pos];
        if c == '(' {
            self.pos += 1;
            let mut list = Vec::new();
            loop {
                self.skip_whitespace_and_comments();
                if self.pos >= self.chars.len() {
                    return Err("Unclosed parenthesis '(' in S-expression".to_string());
                }
                if self.chars[self.pos] == ')' {
                    self.pos += 1;
                    break;
                }
                list.push(self.parse_sexpr()?);
            }
            Ok(SExpr::List(list))
        } else if c == ')' {
            Err(format!("Unexpected ')' at character offset {}", self.pos))
        } else if c == '"' {
            self.parse_string()
        } else if c == '?' {
            // Character literal, e.g. ?্ or ?a
            self.pos += 1;
            if self.pos >= self.chars.len() {
                return Err("Unexpected EOF after '?' in character literal".to_string());
            }
            let ch = self.chars[self.pos];
            self.pos += 1;
            Ok(SExpr::Char(ch))
        } else {
            self.parse_atom()
        }
    }

    fn parse_string(&mut self) -> Result<SExpr, String> {
        self.pos += 1; // skip opening "
        let mut s = String::new();
        while self.pos < self.chars.len() {
            let c = self.chars[self.pos];
            if c == '"' {
                self.pos += 1;
                return Ok(SExpr::String(s));
            } else if c == '\\' {
                self.pos += 1;
                if self.pos >= self.chars.len() {
                    return Err("Unexpected EOF in string escape sequence".to_string());
                }
                let esc = self.chars[self.pos];
                self.pos += 1;
                match esc {
                    'n' => s.push('\n'),
                    'r' => s.push('\r'),
                    't' => s.push('\t'),
                    '"' => s.push('"'),
                    '\\' => s.push('\\'),
                    _ => {
                        s.push('\\');
                        s.push(esc);
                    }
                }
            } else {
                s.push(c);
                self.pos += 1;
            }
        }
        Err("Unterminated string literal".to_string())
    }

    fn parse_atom(&mut self) -> Result<SExpr, String> {
        let mut atom = String::new();
        while self.pos < self.chars.len() {
            let c = self.chars[self.pos];
            if c.is_whitespace() || c == '(' || c == ')' || c == ';' || c == '"' {
                break;
            }
            atom.push(c);
            self.pos += 1;
        }

        if atom.is_empty() {
            return Err(format!("Empty atom at character offset {}", self.pos));
        }

        // Try parsing as integer
        if let Ok(num) = atom.parse::<i64>() {
            return Ok(SExpr::Int(num));
        }

        Ok(SExpr::Symbol(atom))
    }
}

